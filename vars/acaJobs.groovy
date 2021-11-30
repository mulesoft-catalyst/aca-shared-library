#!/usr/bin/groovy
import groovy.json.JsonSlurper


def printMessage(message){
  echo "${message}"
}

def applyCanaryPolicy(String organizationId, String groupId, String assetId, String assetName, String assetVersion, String assetClassifier, String apiVersion, String host, String port, String protocol, String path, String weight,
                      String hostCanary, String portCanary, String protocolCanary, String pathCanary, String weightCanary){
  //Auth API config
  def authAPIEndpoint = "https://anypoint.mulesoft.com/accounts/api/v2/oauth2/token"
  def ANYPOINT_CONNECTED_APP_CREDENTIALS_USR = "1726d936b1d14b1f9a23282f0e5a7330" //TODO: externalize into credentials
  def ANYPOINT_CONNECTED_APP_CREDENTIALS_PWD = "5B02329f8D264ec9822fFc344BFd405f" //TODO: externalize into credentials

  //API Manager API config
  def apiManagerEndpoint = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations"
  def environmentId = "eb473ffd-2134-4ecf-b7bc-63a5d0856743"

  //Step 1 - Create a Proxy app (optional)
  echo "applyCanaryPolicy Step 1"
  def proxyApiId= createProxy("${organizationId}", "${groupId}", "${assetId}", "${assetVersion}", "${assetName}", "${assetClassifier}", "${apiVersion}")

  //Step 2 - Apply the policy
  echo "applyCanaryPolicy Step 2"

  def postBody = [
      configurationData: [
        host: "${host}",
        port: "${port}",
        protocol: "${protocol}",
        path: "${path}",
        weight: "${weight}",
        hostCanary: "${hostCanary}",
        portCanary: "${portCanary}",
        protocolCanary: "${protocolCanary}",
        pathCanary: "${pathCanary}",
        weightCanary: "${weightCanary}"
      ],
      id: null,
      pointcutData: null,
      apiVersionId: "${proxyApiId}",
      groupId: "${groupId}",
      assetId: "${assetId}",
      assetVersion: "${assetVersion}"
  ]

  def jsonBody = groovy.json.JsonOutput.toJson(postBody)
  echo "jsonBody: " + jsonBody

  def localPoliciesUrl = "${apiManagerEndpoint}/${organizationId}/environments/${environmentId}/apis/${proxyApiId}/policies"
  println "${localPoliciesUrl}"
  println "${localPoliciesUrl}.trim()"
  def post = new URL(localPoliciesUrl).openConnection()
  post.setRequestMethod("POST")
  post.setDoOutput(true)
  post.setRequestProperty ("Authorization", "Bearer " + getAuthToken("${authAPIEndpoint}", "${ANYPOINT_CONNECTED_APP_CREDENTIALS_USR}", "${ANYPOINT_CONNECTED_APP_CREDENTIALS_PSW}"))
  post.setRequestProperty("Content-Type", "application/json")
  post.getOutputStream().write(jsonBody.getBytes("UTF-8"))

  def http_code = post.getResponseCode()
  def responseTxt = post.getInputStream().getText()

  echo "http_code: ${http_code}, response: ${responseTxt}"

  // fail step if graph call fails
  assert http_code.equals(201) : "Apply Canary policy response should be a 201 but received ${http_code}! -> ${responseTxt}"
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

def createProxy(String organizationId, String groupId, String assetId, String assetVersion, String assetName, String assetClassifier, String apiVersion){

  //Auth API config
  def authAPIEndpoint = "https://anypoint.mulesoft.com/accounts/api/v2/oauth2/token"
  def ANYPOINT_CONNECTED_APP_CREDENTIALS_USR = "1726d936b1d14b1f9a23282f0e5a7330" //TODO: externalize into credentials
  def ANYPOINT_CONNECTED_APP_CREDENTIALS_PSW = "5B02329f8D264ec9822fFc344BFd405f" //TODO: externalize into credentials

  //API Manager API config
  def apiManagerEndpoint = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations"
  def environmentId = "eb473ffd-2134-4ecf-b7bc-63a5d0856743"

  def boundary =  '----abcd' + Long.toString(System.currentTimeMillis()) * 2 + 'dcba'
  def exchangeAssetsUrl = "https://anypoint.mulesoft.com/exchange/api/v1/assets"

  def authToken=getAuthToken("${authAPIEndpoint}", "${ANYPOINT_CONNECTED_APP_CREDENTIALS_USR}", "${ANYPOINT_CONNECTED_APP_CREDENTIALS_PSW}")
  echo "Bearer ${authToken}"

  //Step 1) Create a base prx asset (201 only if the first time). TODO: implement idempotency as this step is considering we should always create an asset in Exchange
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

  //Step 2) Create Endpoint with a Proxy
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
  return "${out.toString()}"

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

//TODO: Externalize into a separate shared library. Repurpose to use executePOSTBash
def getAuthToken(String oAuthUrl, String clientId, String clientSecret) {
  return sh (script: "curl \
    -s ${oAuthUrl} \
    -X POST \
    -H 'Content-Type: application/json' \
    -d '{\"grant_type\": \"client_credentials\", \"client_id\": \"${clientId}\", \"client_secret\": \"${clientSecret}\"}' \
    | sed -n 's|.*\"access_token\":\"\\([^\"]*\\)\".*|\\1|p'", returnStdout: true).trim()
}
