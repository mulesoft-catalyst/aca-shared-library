#!/usr/bin/groovy

//Author: Gaston Panizza
//Date: November 2021
//Description: Functions definition used across the Automated Canary Analysis Library. Used by the acaPipeline.groovy
//This script was created for research purposes. By no means should be used as-is without understanding what it does and the risks involved.
//Likewise, future modifications must be made by whoever uses it

import groovy.json.JsonSlurper
def authToken=''

def obtainAPToken(){
  authToken=commons.getAuthToken()
}

/*
  Goal: Perform required steps in Anypoint Platform using Platform APIs. It uploads a new asset to Exchange, creates an API instance,
  applies a policy and deploys a proxy app
*/
def String applyCanaryPolicy(String organizationId, String environmentId, String groupId, String assetId, String assetName, String assetVersion, String assetClassifier, String apiVersion, String assetIdPolicy, String assetVersionPolicy,
                      String host, String port, String protocol, String path, String weight,
                      String hostCanary, String portCanary, String protocolCanary, String pathCanary, String weightCanary){

  //Step 1 - Create a Proxy app (optional)
  def proxyApiId= createProxy("${organizationId}", "${environmentId}", "${groupId}", "${assetId}", "${assetVersion}", "${assetName}", "${assetClassifier}", "${apiVersion}")

  //Step 2 - Apply the policy
  def policyId = applyPolicy("${organizationId}", "${environmentId}", "${groupId}", "${assetIdPolicy}", "${assetVersionPolicy}", "${proxyApiId}", "${host}", "${port}", "${protocol}", "${path}", "${weight}", "${hostCanary}", "${portCanary}", "${protocolCanary}", "${pathCanary}", "${weightCanary}")

  //Step 3 - Deploy the proxy (optional)
  def appId = deployCreatedProxy("${organizationId}", "${environmentId}", "${assetId}", "${proxyApiId}")

  return ["${proxyApiId}", "${policyId}", "${appId}"]
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

      /*println "Newman Command: ${command}"
      def process = [ 'bash', '-c', "${command}" ].execute()
      def out = new ByteArrayOutputStream()
      def err = new ByteArrayOutputStream()
      process.consumeProcessOutput(out, err)
      process.waitFor()
      println "Output is: ${out.toString()}"
      println "Error is: ${err.toString()}"
      String response = out.toString()
      process = null
      err = null
      out = null*/

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
def String executeCanaryAnalysis(String canaryServerProtocol, String canaryServer, String canaryServerPort, String appName){
  String canaryConfig='{\"canaryConfig\":{\"name\":\"canary-config-prometheus\",\"description\":\"Configuration for Prometheus\",\"configVersion\":\"1\",\"applications\":[\"ad-hoc\"],\"judge\":{\"name\":\"NetflixACAJudge-v1.0\",\"judgeConfigurations\":{}},\"metrics\":[{\"name\":\"Avg Response Time\",\"query\":{\"type\":\"prometheus\",\"customInlineTemplate\":\"PromQL:avg(avg_over_time(cloudhub_prometheus_rt{instance=\\\"prometheus-metrics.us-e2.cloudhub.io:80\\\", job=\\\"cloudhub-metrics\\\", canary=\\\"${scope}\\\"}[120m]))\",\"serviceType\":\"prometheus\"},\"groups\":[\"Canaries\"],\"analysisConfigurations\":{\"canary\":{\"critical\":false,\"nanStrategy\":\"replace\",\"effectSize\":{\"allowedIncrease\":1,\"allowedDecrease\":1},\"outliers\":{\"strategy\":\"keep\"},\"direction\":\"decrease\"}},\"scopeName\":\"default\"},{\"name\":\"Success Rate\",\"query\":{\"type\":\"prometheus\",\"customInlineTemplate\":\"PromQL:sum(sum_over_time(cloudhub_prometheus{instance=\\\"prometheus-metrics.us-e2.cloudhub.io:80\\\", job=\\\"cloudhub-metrics\\\", http_code=\\\"200\\\", canary=\\\"${scope}\\\"}[120m]))\",\"serviceType\":\"prometheus\"},\"groups\":[\"Canaries\"],\"analysisConfigurations\":{\"canary\":{\"nanStrategy\":\"replace\",\"critical\":false,\"effectSize\":{\"allowedIncrease\":1,\"allowedDecrease\":1},\"outliers\":{\"strategy\":\"keep\"},\"direction\":\"decrease\"}},\"scopeName\":\"default\"}],\"templates\":{},\"classifier\":{\"groupWeights\":{\"Canaries\":100}}},\"executionRequest\":{\"scopes\":[{\"scopeName\":\"default\",\"controlScope\":0,\"controlLocation\":\"us-east-1\",\"controlOffsetInMinutes\":\"10\",\"experimentScope\":1,\"experimentLocation\":\"us-east-1\",\"startTimeIso\": \"' + "${params.startTimeIso}" + '\",\"endTimeIso\": \"' + "${params.endTimeIso}" + '\",\"step\":2,\"extendedScopeParams\":{}}],\"thresholds\":{\"pass\":95,\"marginal\":75}}}'
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
def String retrieveAnalysisResults(String canaryServerProtocol, String canaryServer, String canaryServerPort, String analysisId){
  def url = "${canaryServerProtocol}://${canaryServer}:${canaryServerPort}/standalone_canary_analysis/${analysisId}";
  def get = new URL(url).openConnection();
  get.setRequestMethod("GET")
  get.setRequestProperty("Accept", '*/*')
  get.setRequestProperty("Content-Type", "application/json")
  def getRC = get.getResponseCode();

  return get.getInputStream().getText()
}

/*
  Goal: Takes decisions according the ACA result
*/
def decideBasedOnResults(String analysisResult, String proxyApiId, String policyId){
  // Suggestions: If sucessful --> Notify distribution list. If fail --> Rollback steps from applyCanaryPolicy and notify distribution list
  def slurper = new JsonSlurper()
  def result = slurper.parseText(analysisResult)
  if(result.complete == true){
    if(result.canaryAnalysisExecutionResult.didPassThresholds){
      //Increase traffic
      println "Increasing traffic weight to Canary"
      updateCanaryTraffic("${params.organizationId}", "${params.environmentId}", "${proxyApiId}", "${policyId}",
                          "${params.host}", "${params.port}", "${params.protocol}", "${params.path}", "${params.weightBaseSuccessful}", "${params.hostCanary}", "${params.portCanary}", "${params.protocolCanary}", "${params.pathCanary}", "${params.weightCanarySuccessful}")
    }else{
      //Rollback Canary
      println "Rollbacking Canary"
      //Rollback the API Manager app
      rollBackCreatedProxy("${params.organizationId}", "${params.environmentId}", "${params.assetName}")
      //Rollback the API Manager instance
      rollbackProxyInstance("${params.organizationId}", "${params.environmentId}", "${proxyApiId}")
    }
  }
}

/*
  Goal: Uploads asset to Exchange and create the API Manager instance
*/
def String createProxy(String organizationId, String environmentId, String groupId, String assetId, String assetVersion, String assetName, String assetClassifier, String apiVersion){

  //API Manager API config
  def apiManagerEndpoint = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations"

  def exchangeAssetsUrl = "https://anypoint.mulesoft.com/exchange/api/v1/assets"

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
def String applyPolicy(String organizationId, String environmentId, String groupId, String assetId, String assetVersion, String proxyApiId,
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

  def localPoliciesUrl = "${apiManagerEndpoint}/${organizationId}/environments/${environmentId}/apis/${proxyApiId}/policies"

  def response = commons.executePostWithBody("${localPoliciesUrl}", "${authToken}", "${postBody}", "201", "applyCanaryPolicy - Step 2")

  def slurper = new JsonSlurper()
  def result = slurper.parseText(response)
  def policyId = result.id
  println "Created Policy ID is: ${policyId.toString().trim()}"
  return "${policyId}"
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

  def deploymentsUrl = "${apiProxiesEndpoint}/${organizationId}/environments/${environmentId}/apis/${proxyApiId}/deployments"
  def response = commons.executePostWithBody("${deploymentsUrl}", "${authToken}", "${postBody}", "201", "applyCanaryPolicy - Step 3")
  def slurper = new JsonSlurper()
  def result = slurper.parseText(response)
  def appId = result.id
  println "Created App ID is: ${appId.toString().trim()}"
  return "${appId}"
  //return "${response}"
}

//TODO: move to commons and make extra headers an optional step of the executeDelete function
def rollBackCreatedProxy(String organizationId, String environmentId, String assetName){
  //TODO refactor
  def applicationsEndpoint = "https://anypoint.mulesoft.com/cloudhub/api/applications/${assetName}"
  String curlCommand = "curl \
  -w 'HTTPSTATUS:%{http_code}' \
  -X DELETE ${applicationsEndpoint} \
  -H 'Authorization: Bearer ${authToken}' \
  -H 'X-ANYPNT-ENV-ID: ${environmentId}' \
  -H 'X-ANYPNT-ORG-ID: ${organizationId}' \
  -H 'Content-Type: application/json' "

  String response = commons.executeDelete("${curlCommand}", "204", "rollBackCreatedProxy")
  body = null
  policiesUrl = null
  println "${response}"
}

def rollbackProxyInstance(String organizationId, String environmentId, String proxyApiId){
  //TODO refactor
  def apiManagerEndpoint = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations/${organizationId}/environments/${environmentId}/apis/${proxyApiId}"
  String curlCommand = "curl \
  -w 'HTTPSTATUS:%{http_code}' \
  -X DELETE ${apiManagerEndpoint} \
  -H 'Authorization: Bearer ${authToken}' \
  -H 'Content-Type: application/json' "
  String response = commons.executeDelete("${curlCommand}", "204", "rollbackProxyInstance")
  body = null
  policiesUrl = null
  println "${response}"
}

def updateCanaryTraffic(String organizationId, String environmentId, String proxyApiId, String policyId,
                        String host, String port, String protocol, String path, String weightBase,
                        String hostCanary, String portCanary, String protocolCanary, String pathCanary, String weightCanary){
  def policiesUrl = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations/${organizationId}/environments/${environmentId}/apis/${proxyApiId}/policies/${policyId}"
  def body = """{
      "configurationData": {
        "host": "${host}",
        "port": "${port}",
        "protocol": "${protocol}",
        "path": "${path}",
        "weight": "${weightBase}",
        "hostCanary": "${hostCanary}",
        "portCanary": "${portCanary}",
        "protocolCanary": "${protocolCanary}",
        "pathCanary": "${pathCanary}",
        "weightCanary": "${weightCanary}"
    }
  }"""
  String response = commons.executePatchWithBody("${policiesUrl}", "${body}", "${authToken}")
  body = null
  policiesUrl = null
  println "${response}"
}
