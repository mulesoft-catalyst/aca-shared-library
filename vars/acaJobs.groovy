#!/usr/bin/groovy
import groovy.json.JsonSlurper

//Auth API config
def authAPIEndpoint = "https://anypoint.mulesoft.com/accounts/api/v2/oauth2/token"
def ANYPOINT_CONNECTED_APP_CREDENTIALS_USR = "1726d936b1d14b1f9a23282f0e5a7330" //TODO: externalize into credentials
def ANYPOINT_CONNECTED_APP_CREDENTIALS_PWD = "5B02329f8D264ec9822fFc344BFd405f" //TODO: externalize into credentials

//API Manager API config
def apiManagerEndpoint = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations"
def orgId = "9033ff23-884a-4352-b75b-14fc8237b2c4"
def envId = "eb473ffd-2134-4ecf-b7bc-63a5d0856743"

//Canary proxy Exchange Asset (should be externalized to parametrized pipeline)
def assetName="canary-release-prx"
def assetClassifier="http"
def apiVersion="v1"

def printMessage(message){
  echo "${message}"
}

def applyCanaryPolicy(String groupId, String assetId, String assetVersion, String host, Integer port, String protocol, String path, String weight,
                      String hostCanary, Integer portCanary, String protocolCanary, String pathCanary, String weightCanary){

  //Step 1 - Create a Proxy app (optional)
  createProxy(${orgId}, ${groupId}, ${assetId}, ${assetVersion}, ${assetName}, ${assetClassifier}, ${apiVersion})

  //Step 3 - Apply the policy
  def postBody = [
      configurationData: [
        host: ${host},
        port: ${port},
        protocol: ${protocol},
        path: ${path},
        weight: ${weight},
        hostCanary: ${hostCanary},
        portCanary: ${portCanary},
        protocolCanary: ${protocolCanary},
        pathCanary: ${pathCanary},
        weightCanary: ${weightCanary}
      ],
      id: null,
      pointcutData: null,
      apiVersionId: API_ID,
      groupId: ${groupId},
      assetId: ${assetId},
      assetVersion: ${assetVersion}
  ]

  def jsonBody = groovy.json.JsonOutput.toJson(postBody)
  echo "jsonBody: " + jsonBody

  def localPoliciesUrl = "${apiManagerEndpoint}/${orgId}/environments/${ENVIRONMENT_ID}/apis/${API_ID}/policies"

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

//TODO: Externalize into a separate shared library
def getAuthToken(String oAuthUrl, String clientId, String clientSecret) {
  return sh (script: "curl \
    -s ${oAuthUrl} \
    -X POST \
    -H 'Content-Type: application/json' \
    -d '{\"grant_type\": \"client_credentials\", \"client_id\": \"${clientId}\", \"client_secret\": \"${clientSecret}\"}' \
    | sed -n 's|.*\"access_token\":\"\\([^\"]*\\)\".*|\\1|p'", returnStdout: true).trim()
}

def createProxy(String organizationId, String groupId, String assetId, String assetVersion, String assetName, String assetClassifier, String apiVersion){
  def boundary =  'abcd' + Long.toString(System.currentTimeMillis()) * 2 + 'dcba'
  def twoHyphens = '--'
  def lineEnd = '\r\n'
  def exchangeAssetsUrl = "https://anypoint.mulesoft.com/exchange/api/v1/assets"

  def authToken=getAuthToken("${authAPIEndpoint}", "${ANYPOINT_CONNECTED_APP_CREDENTIALS_USR}", "${ANYPOINT_CONNECTED_APP_CREDENTIALS_PSW}")

  //Step 1) Create a base prx asset (201 only if the first time). TODO: implement idempotency as this step is cconsidering we should always create an asset in Exchange
  def connection = new URL(exchangeAssetsUrl).openConnection()
  connection.setRequestMethod("POST")
  connection.setDoOutput(true)
  connection.setRequestProperty ("Authorization", "Bearer ${authToken}")
  connection.setRequestProperty("Content-Type", "multipart/form-data; boundary="" + boundary)

  def outputStream = new DataOutputStream(connection.getOutputStream())
  outputStream.writeBytes(twoHyphens + boundary + lineEnd);
  outputStream.writeBytes('Content-Disposition: form-data; name="organizationId"' + lineEnd)
  outputStream.writeBytes('Content-Type: text/plain; charset=UTF-8' + lineEnd)
  outputStream.writeBytes('Content-Transfer-Encoding: 8bit' + lineEnd)
  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(${organizationId})

  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(twoHyphens + boundary + lineEnd)
  outputStream.writeBytes('Content-Disposition: form-data; name="groupId"' + lineEnd)
  outputStream.writeBytes('Content-Type: text/plain; charset=UTF-8' + lineEnd)
  outputStream.writeBytes('Content-Transfer-Encoding: 8bit' + lineEnd)
  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(${groupId})

  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(twoHyphens + boundary + lineEnd)
  outputStream.writeBytes('Content-Disposition: form-data; name="assetId"' + lineEnd)
  outputStream.writeBytes('Content-Type: text/plain; charset=UTF-8' + lineEnd)
  outputStream.writeBytes('Content-Transfer-Encoding: 8bit' + lineEnd)
  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(${assetId})

  outputStream.writeBytes(lineEnd)
	outputStream.writeBytes(twoHyphens + boundary + lineEnd)
	outputStream.writeBytes('Content-Disposition: form-data; name="version"' + lineEnd)
	outputStream.writeBytes('Content-Type: text/plain; charset=UTF-8' + lineEnd)
	outputStream.writeBytes('Content-Transfer-Encoding: 8bit' + lineEnd)
	outputStream.writeBytes(lineEnd)
	outputStream.writeBytes(${assetVersion})

  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(twoHyphens + boundary + lineEnd)
  outputStream.writeBytes('Content-Disposition: form-data; name="name"' + lineEnd)
  outputStream.writeBytes('Content-Type: text/plain; charset=UTF-8' + lineEnd)
  outputStream.writeBytes('Content-Transfer-Encoding: 8bit' + lineEnd)
  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(${assetName})

  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(twoHyphens + boundary + lineEnd)
  outputStream.writeBytes('Content-Disposition: form-data; name="classifier"' + lineEnd)
  outputStream.writeBytes('Content-Type: text/plain; charset=UTF-8' + lineEnd)
  outputStream.writeBytes('Content-Transfer-Encoding: 8bit' + lineEnd)
  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(${assetClassifier})

  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(twoHyphens + boundary + lineEnd)
  outputStream.writeBytes('Content-Disposition: form-data; name="apiVersion"' + lineEnd)
  outputStream.writeBytes('Content-Type: text/plain; charset=UTF-8' + lineEnd)
  outputStream.writeBytes('Content-Transfer-Encoding: 8bit' + lineEnd)
  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(${apiVersion})

  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(twoHyphens + boundary + lineEnd)
  outputStream.writeBytes('Content-Disposition: form-data; name="asset"' + lineEnd)
  outputStream.writeBytes('Content-Type: text/plain; charset=UTF-8' + lineEnd)
  outputStream.writeBytes('Content-Transfer-Encoding: 8bit' + lineEnd)
  outputStream.writeBytes(lineEnd)
  outputStream.writeBytes(${apiVersion})

  outputStream.writeBytes(lineEnd)
	outputStream.writeBytes(twoHyphens + boundary + lineEnd)
	outputStream.writeBytes('Content-Disposition: form-data; name="asset"' + lineEnd)
	outputStream.writeBytes('Content-Type: application/octet-stream' + lineEnd)
	outputStream.writeBytes('Content-Transfer-Encoding: binary' + lineEnd)
	outputStream.writeBytes(lineEnd)
  outputStream.writeBytes("undefined")

  outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)

  outputStream.flush()
	outputStream.close()

  def http_code = connection.getResponseCode()
  def responseTxt = connection.getInputStream().getText()

  echo "http_code: ${http_code}, response: ${responseTxt}"

  // fail step if graph call fails
  assert http_code.equals(201) : "Upload Exchange Asset response should be a 201 but received ${http_code}! -> ${responseTxt}"

  //Step 2) Create Endpoint with a Proxy
  def postBody = [
      endpoint: [
        deploymentType: "CH",
        "isCloudHub":true,
        "muleVersion4OrAbove":true,
        "proxyUri":"http://0.0.0.0:8081/200",
        "proxyTemplate":[
           "assetVersion":"2.0.2"
        ],
        "referencesUserDomain":false,
        "responseTimeout":null,
        "type":"http",
        "uri":"https://httpstat.us",
        "validation":"NOT_APPLICABLE"
      ],
      "providerId":null,
      "spec":[
          "assetId":${assetId},,
          "groupId":${groupId},
           ${assetVersion}
       ]
  ]

  def jsonBody = groovy.json.JsonOutput.toJson(postBody)
  echo "jsonBody: " + jsonBody

  def endpointWithProxyUrl = "${apiManagerEndpoint}/${orgId}/environments/${ENVIRONMENT_ID}/apis"

  def post = new URL(endpointWithProxyUrl).openConnection()
  post.setRequestMethod("POST")
  post.setDoOutput(true)
  post.setRequestProperty ("Authorization", "Bearer ${authToken}")
  post.setRequestProperty("Content-Type", "application/json")
  post.getOutputStream().write(jsonBody.getBytes("UTF-8"))

  http_code = post.getResponseCode()
  responseTxt = post.getInputStream().getText()

  echo "http_code: ${http_code}, response: ${responseTxt}"

  // fail step if graph call fails
  assert http_code.equals(201) : "Create Endpoint with a Proxy response should be a 201 but received ${http_code}! -> ${responseTxt}"

}
