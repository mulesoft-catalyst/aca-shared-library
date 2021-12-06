#!/usr/bin/groovy

//Author: Gaston Panizza
//Date: November 2021
//Description: Functions definition used across the Automated Canary Analysis Library. Used by the acaPipeline.groovy
//This script was created for research purposes. By no means should be used as-is without understanding what it does and the risks involved.
//Likewise, future modifications must be made by whoever uses it

import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput

/*
  Goal: Perform required steps in Anypoint Platform using Platform APIs. It uploads a new asset to Exchange, creates an API instance,
  applies a policy and deploys a proxy app
*/
def applyCanaryPolicy(String organizationId, String environmentId, String groupId, String assetId, String assetName, String assetVersion, String assetClassifier, String apiVersion, String assetIdPolicy, String assetVersionPolicy,
                      String host, String port, String protocol, String path, String weight,
                      String hostCanary, String portCanary, String protocolCanary, String pathCanary, String weightCanary){

  //Step 1 - Create a Proxy app (optional)
  def proxyApiId= createProxy("${organizationId}", "${environmentId}", "${groupId}", "${assetId}", "${assetVersion}", "${assetName}", "${assetClassifier}", "${apiVersion}")

  //Step 2 - Apply the policy
  def applyPolicyResponse = applyPolicy("${organizationId}", "${environmentId}", "${groupId}", "${assetIdPolicy}", "${assetVersionPolicy}", "${proxyApiId}", "${host}", "${port}", "${protocol}", "${path}", "${weight}", "${hostCanary}", "${portCanary}", "${protocolCanary}", "${pathCanary}", "${weightCanary}")

  //Step 3 - Deploy the proxy (optional)
  def deployProxyResponse = deployCreatedProxy("${organizationId}", "${environmentId}", "${assetId}", "${proxyApiId}")
}

/*
  Goal: Executes a Load Testing to collect enough data points to perform an ACA
*/
def executeLoadTesting(String newmanPath, String newmanCollection, String newmanIterations, String reportPath, String reportFilename){
  dir("${WORKSPACE}"){
    String command = """${newmanPath} run ${newmanCollection} \
      --env-var PROTOCOL=${params.endpointProtocol} \
      --env-var URL=${params.endpointHost} \
      --env-var RESOURCE=${params.endpointResource} \
      -n ${newmanIterations} \
      -r htmlextra \
      --reporter-htmlextra-title "Automated Canary Analysis Load Test" \
      --reporter-htmlextra-export ${reportPath}/${reportFilename} \
      --suppress-exit-code"""

    commons.executeSh(command)

    publishHTML( target:
    [
      allowMissing: true,
      alwaysLinkToLastBuild: false,
      keepAll: false,
      reportDir: "${reportPath}",
      reportFiles: "${reportFilename}",
      reportName: 'Canary Load Test Report',
      reportTitles: ''
    ]
    )
  }
}

/*
  Goal: Executes an Async ACA
*/
def executeCanaryAnalysis(String canaryServerProtocol, String canaryServer, String canaryServerPort, String canaryConfig, String appName){
  def post = new URL("${canaryServerProtocol}://${canaryServer}:${canaryServerPort}/standalone_canary_analysis/?metricsAccountName=canary-prometheus&storageAccountName=in-memory-store-account&application=${appName}").openConnection();
  def message = "${canaryConfig}"
  post.setRequestMethod("POST")
  post.setDoOutput(true)
  post.setRequestProperty("Accept", '*/*')
  post.setRequestProperty("Content-Type", "application/json")
  post.getOutputStream().write(message.getBytes("UTF-8"));
  def postRC = post.getResponseCode();

  def slurper = new JsonSlurper()
  def result = slurper.parseText(post.getInputStream().getText())

  return result.canaryAnalysisExecutionId;
}

/*
  Goal: Retrieves the result for a given ACA
*/
def retrieveAnalysisResults(String canaryServerProtocol, String canaryServer, String canaryServerPort, String analysisId){
  def url = "${canaryServerProtocol}://${canaryServer}:${canaryServerPort}/standalone_canary_analysis/${analysisId}";
  def get = new URL(url).openConnection();
  get.setRequestMethod("GET")
  get.setRequestProperty("Accept", '*/*')
  get.setRequestProperty("Content-Type", "application/json")
  def getRC = get.getResponseCode();

  def slurper = new JsonSlurperClassic()
  def result = slurper.parseText(get.getInputStream().getText())

  return result.canaryAnalysisExecutionResult;
}

/*
  Goal: Takes decisions according the ACA result
*/
def decideBasedOnResults(analysisResult){
  //TODO: Implement logic according two scenarios: Analysis was successful and Analysis failed
  // Suggestions: If sucessful --> Notify distribution list. If fail --> Rollback steps from applyCanaryPolicy and notify distribution list
  //def slurper = new JsonSlurper()
  //def result = slurper.parseText(analysisResult)

  println "${analysisResult}"
  println JsonOutput.prettyPrint(JsonOutput.toJson(analysisResult.didPassThresholds))
  echo "ok"
}

/*
  Goal: Uploads asset to Exchange and create the API Manager instance
*/
def createProxy(String organizationId, String environmentId, String groupId, String assetId, String assetVersion, String assetName, String assetClassifier, String apiVersion){

  //API Manager API config
  def apiManagerEndpoint = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations"

  def exchangeAssetsUrl = "https://anypoint.mulesoft.com/exchange/api/v1/assets"

  def authToken=commons.getAuthToken()

  //Step a) Create a base prx asset (201 only if the first time). TODO: implement idempotency as this step is considering we should always create an asset in Exchange
    String curlCommand = "curl \
    -w 'HTTPSTATUS:%{http_code}' \
    -X POST ${exchangeAssetsUrl} \
    -H 'Authorization: Bearer ${authToken}' \
    -H 'Content-Type: multipart/form-data' \
    -F 'organizationId=${organizationId}' \
    -F 'groupId=${organizationId}' \
    -F 'assetId=${assetId}' \
    -F 'version=${assetVersion}' \
    -F 'name=${assetName}' \
    -F 'classifier=${assetClassifier}' \
    -F 'apiVersion=${apiVersion}' \
    -F 'asset=\"undefined\"' "

  def uploadToExchangeResponseObj = commons.executePostWithMultipart("${curlCommand}", "201", "createProxy - Upload Asset to Exchange")

  //Step b) Create Endpoint with a Proxy
  def postBody = """
  {
      "endpoint": {
        "isCloudHub":true,
        "muleVersion4OrAbove":true,
        "proxyUri":"http://0.0.0.0:8081/200",
        "proxyTemplate": {
           "assetVersion":"2.0.2"
        },
        "type":"http",
        "uri":"https://httpstat.us",
        "validation":"NOT_APPLICABLE"
      },
      "instanceLabel": null,
      "spec":{
          "assetId":"${assetId}",
          "groupId":"${organizationId}",
          "version":"${assetVersion}"
       }
  }
  """

  def endpointWithProxyUrl = "${apiManagerEndpoint}/${organizationId}/environments/${environmentId}/apis"
  def apiInstanceCreationResponseObj = commons.executePostWithBody("${endpointWithProxyUrl}", "${authToken}", "${postBody}", "201", "createProxy - Proxy Instance")

  def out = new ByteArrayOutputStream()
  def err = new ByteArrayOutputStream()
  def proc = ['bash', '-c', "echo '${apiInstanceCreationResponseObj}' | sed -n 's|.*\"apiId\":\\([^\"]*\\)},.*|\\1|p'"].execute()
  proc.consumeProcessOutput(out, err)
  proc.waitFor()

  println "Created API ID is: ${out.toString()}"
  return "${out.toString().trim()}"

}

/*
  Goal: Applies a policy to the created API Instance
*/
def applyPolicy(String organizationId, String environmentId, String groupId, String assetId, String assetVersion, String proxyApiId,
                String host, String port, String protocol, String path, String weight,
                String hostCanary, String portCanary, String protocolCanary, String pathCanary, String weightCanary){

  def apiManagerEndpoint = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations"

  def postBody = """
  {
      "configurationData": {
        "host": "${host}",
        "port": "${port}",
        "protocol": "${protocol}",
        "path": "${path}",
        "weight": "${weight}",
        "hostCanary": "${hostCanary}",
        "portCanary": "${portCanary}",
        "protocolCanary": "${protocolCanary}",
        "pathCanary": "${pathCanary}",
        "weightCanary": "${weightCanary}"
      },
      "id": null,
      "pointcutData": null,
      "apiVersionId": "${proxyApiId}",
      "groupId": "${groupId}",
      "assetId": "${assetId}",
      "assetVersion": "${assetVersion}"
  }
  """

  def jsonBody = groovy.json.JsonOutput.toJson(postBody)

  def localPoliciesUrl = "${apiManagerEndpoint}/${organizationId}/environments/${environmentId}/apis/${proxyApiId}/policies"

  def authToken=commons.getAuthToken()
  def response = commons.executePostWithBody("${localPoliciesUrl}", "${authToken}", "${postBody}", "201", "applyCanaryPolicy - Step 2")
  return "${response}"
}

def deployCreatedProxy(String organizationId, String environmentId, String assetId, String proxyApiId){
  def apiProxiesEndpoint = "https://anypoint.mulesoft.com/proxies/xapi/v1/organizations"

  def postBody = """
  {
      "applicationName": "${assetId}",
      "gatewayVersion":"4.4.0",
      "overwrite":true,
      "type":"CH",
      "environmentId": "${environmentId}",
      "environmentName":"Sandbox",
      "expectedStatus":"deployed"
  }
  """

  def jsonBody = groovy.json.JsonOutput.toJson(postBody)

  def deploymentsUrl = "${apiProxiesEndpoint}/${organizationId}/environments/${environmentId}/apis/${proxyApiId}/deployments"

  def authToken=commons.getAuthToken()
  def response = commons.executePostWithBody("${deploymentsUrl}", "${authToken}", "${postBody}", "201", "applyCanaryPolicy - Step 3")
  return "${response}"
}
