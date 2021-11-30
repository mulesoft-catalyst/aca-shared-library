#!/usr/bin/groovy
import groovy.json.JsonSlurper

def applyCanaryPolicy(String organizationId, String environmentId, String groupId, String assetId, String assetName, String assetVersion, String assetClassifier, String apiVersion, String assetIdPolicy, String assetVersionPolicy,
                      String host, String port, String protocol, String path, String weight,
                      String hostCanary, String portCanary, String protocolCanary, String pathCanary, String weightCanary){

  //Step 1 - Create a Proxy app (optional)
  echo "applyCanaryPolicy Step 1"
  def proxyApiId= createProxy("${organizationId}", "${environmentId}", "${groupId}", "${assetId}", "${assetVersion}", "${assetName}", "${assetClassifier}", "${apiVersion}")

  //Step 2 - Apply the policy
  echo "applyCanaryPolicy Step 2"
  def applyPolicyResponse = applyPolicy("${organizationId}", "${environmentId}", "${groupId}", "${assetIdPolicy}", "${assetVersionPolicy}", "${proxyApiId}", "${host}", "${port}", "${protocol}", "${path}", "${weight}", "${hostCanary}", "${portCanary}", "${protocolCanary}", "${pathCanary}", "${weightCanary}")

  //Step 3 - Deploy the proxy (optional)
  echo "applyCanaryPolicy Step 3"
  def deployProxyResponse = deployCreatedProxy("${organizationId}", "${environmentId}", "${assetId}", "${proxyApiId}")
}

def executeLoadTesting(){
  echo "ok"
}

def executeCanaryAnalysis(){
  def post = new URL("http://192.168.0.45:8090/standalone_canary_analysis/?metricsAccountName=canary-prometheus&storageAccountName=in-memory-store-account&application=Canary+Test").openConnection();
  def message = '{\"canaryConfig\":{\"name\":\"canary-config-prometheus\",\"description\":\"Configuration for Prometheus\",\"configVersion\":\"1\",\"applications\":[\"ad-hoc\"],\"judge\":{\"name\":\"NetflixACAJudge-v1.0\",\"judgeConfigurations\":{}},\"metrics\":[{\"name\":\"Avg Response Time\",\"query\":{\"type\":\"prometheus\",\"customInlineTemplate\":\"PromQL:avg(avg_over_time(cloudhub_prometheus_rt{instance=\\\"prometheus-metrics.us-e2.cloudhub.io:80\\\", job=\\\"cloudhub-metrics\\\", canary=\\\"${scope}\\\"}[120m]))\",\"serviceType\":\"prometheus\"},\"groups\":[\"Canaries\"],\"analysisConfigurations\":{\"canary\":{\"critical\":false,\"nanStrategy\":\"replace\",\"effectSize\":{\"allowedIncrease\":1,\"allowedDecrease\":1},\"outliers\":{\"strategy\":\"keep\"},\"direction\":\"decrease\"}},\"scopeName\":\"default\"},{\"name\":\"Success Rate\",\"query\":{\"type\":\"prometheus\",\"customInlineTemplate\":\"PromQL:sum(sum_over_time(cloudhub_prometheus{instance=\\\"prometheus-metrics.us-e2.cloudhub.io:80\\\", job=\\\"cloudhub-metrics\\\", http_code=\\\"200\\\", canary=\\\"${scope}\\\"}[120m]))\",\"serviceType\":\"prometheus\"},\"groups\":[\"Canaries\"],\"analysisConfigurations\":{\"canary\":{\"nanStrategy\":\"replace\",\"critical\":false,\"effectSize\":{\"allowedIncrease\":1,\"allowedDecrease\":1},\"outliers\":{\"strategy\":\"keep\"},\"direction\":\"decrease\"}},\"scopeName\":\"default\"}],\"templates\":{},\"classifier\":{\"groupWeights\":{\"Canaries\":100}}},\"executionRequest\":{\"scopes\":[{\"scopeName\":\"default\",\"controlScope\":0,\"controlLocation\":\"us-east-1\",\"controlOffsetInMinutes\":\"10\",\"experimentScope\":1,\"experimentLocation\":\"us-east-1\",\"startTimeIso\":\"2021-11-17T15:00:00Z\",\"endTimeIso\":\"2021-11-17T19:00:00Z\",\"step\":2,\"extendedScopeParams\":{}}],\"thresholds\":{\"pass\":95,\"marginal\":75}}}'
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

def retrieveAnalysisResults(analysisId){
  def url = "http://192.168.0.45:8090/standalone_canary_analysis/${analysisId}";
  def get = new URL(url).openConnection();
  get.setRequestMethod("GET")
  get.setRequestProperty("Accept", '*/*')
  get.setRequestProperty("Content-Type", "application/json")
  def getRC = get.getResponseCode();
  println(getRC);

  def slurper = new JsonSlurper()
  def result = slurper.parseText(get.getInputStream().getText())

  return result.canaryAnalysisExecutionResult;
}

def decideBasedOnResults(){
  echo "ok"
}

def createProxy(String organizationId, String environmentId, String groupId, String assetId, String assetVersion, String assetName, String assetClassifier, String apiVersion){

  //API Manager API config
  def apiManagerEndpoint = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations"

  def boundary =  '----abcd' + Long.toString(System.currentTimeMillis()) * 2 + 'dcba'
  def exchangeAssetsUrl = "https://anypoint.mulesoft.com/exchange/api/v1/assets"

  def authToken=commons.getAuthToken()
  echo "Bearer ${authToken}"

  //Step a) Create a base prx asset (201 only if the first time). TODO: implement idempotency as this step is considering we should always create an asset in Exchange
  String response = sh (script: "curl \
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
    -F 'asset=\"undefined\"' ", returnStdout: true)

  def http_code = response.split("HTTPSTATUS:")[1]
  println "http code: ${http_code}"

  assert http_code.equals("201") : "Create a base Prx asset response should be a '201' but received ${http_code}! -> ${response}"

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
  print "${postBody}"
  def apiInstanceCreationResponseObj = executePOSTBash("${endpointWithProxyUrl}", "${authToken}", "${postBody}", "201", "createProxy - Proxy Instance")

  def out = new ByteArrayOutputStream()
  def err = new ByteArrayOutputStream()
  def proc = ['bash', '-c', "echo '${apiInstanceCreationResponseObj}' | sed -n 's|.*\"apiId\":\\([^\"]*\\)},.*|\\1|p'"].execute()
  proc.consumeProcessOutput(out, err)
  proc.waitFor()

  println "Created API ID is: ${out.toString()}"
  return "${out.toString().trim()}"

}

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
  echo "jsonBody: " + jsonBody

  def localPoliciesUrl = "${apiManagerEndpoint}/${organizationId}/environments/${environmentId}/apis/${proxyApiId}/policies"
  println "${localPoliciesUrl}"

  def authToken=commons.getAuthToken()
  def response = executePOSTBash("${localPoliciesUrl}", "${authToken}", "${postBody}", "201", "applyCanaryPolicy - Step 2")
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
  echo "jsonBody: " + jsonBody

  def deploymentsUrl = "${apiProxiesEndpoint}/${organizationId}/environments/${environmentId}/apis/${proxyApiId}/deployments"
  println "${deploymentsUrl}"

  def authToken=commons.getAuthToken()
  def response = executePOSTBash("${deploymentsUrl}", "${authToken}", "${postBody}", "201", "applyCanaryPolicy - Step 3")
  return "${response}"
}

//TODO: Reusable fx. Migrate to a different shared-library
def executePOSTBash(String url, String token, String body, String expectedHttpCode, String methodName){
  def process = [ 'bash', '-c', "curl -X POST -d '${body}' -w 'HTTPSTATUS:%{http_code}' -H \"Content-Type: application/json\" -H \"Authorization: Bearer ${token}\" ${url}" ].execute()
  process.waitFor()
  def response = process.text

  def rawResponse = response.split("HTTPSTATUS:")[0]
  println "rawResponse: ${rawResponse}"

  return "${rawResponse}"
}
