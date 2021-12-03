#!/usr/bin/groovy

//Author: Gaston Panizza
//Date: November 2021
//Description: Common functions definition used across the Automated Canary Analysis Library. Can be used as base for other libraries as well
//This pipeline was created by the author specifically for AT&T needs,
//it is responsability of AT&T teams to evolve and maintain these scripts when
//the author is not involved in the project anymore
//Pre-requisites: create a Jenkins credentials called connected-app-credentials, where clientId is stored in the username field and clientSecret in the password field

//Goal: Obtain an access token that can be used to authenticate Anypoint Platform APIs
def getAuthToken() {
  def oAuthUrl = "https://anypoint.mulesoft.com/accounts/api/v2/oauth2/token"
  withCredentials([usernamePassword(credentialsId: "connected-app-credentials", passwordVariable: 'clientSecret', usernameVariable: 'clientId')]) {
         String curlCommand = "curl \
           -s ${oAuthUrl} \
           -X POST \
           -H 'Content-Type: application/json' \
           -d '{\"grant_type\": \"client_credentials\", \"client_id\": \"${clientId}\", \"client_secret\": \"${clientSecret}\"}' \
           | sed -n 's|.*\"access_token\":\"\\([^\"]*\\)\".*|\\1|p'"

         def process = [ 'bash', '-c', "${curlCommand}" ].execute()
         process.waitFor()
         def response = process.text

         def rawResponse = response.split("HTTPSTATUS:")[0]
         println "Token reponse: ${rawResponse}"

         return "${rawResponse}"
  }
}


//Goal: execute a POST request with a body using Curl in a thread
def executePostWithBody(String url, String token, String body, String expectedHttpCode, String methodName){
  String curlCommand="curl -X POST -d '${body}' -w 'HTTPSTATUS:%{http_code}' -H \"Content-Type: application/json\" -H \"Authorization: Bearer ${token}\" ${url}"
  def response = executeSh(curlCommand)

  def rawResponse = response.split("HTTPSTATUS:")[0]
  println "rawResponse: ${rawResponse}"

  return "${rawResponse}"
}

//Goal: execute a multipart POST request using Curl in a thread
def executePostWithMultipart(String curlCommand, String expectedHttpCode, String methodName){
  def response = executeSh(curlCommand)

  def rawResponse = response.split("HTTPSTATUS:")[0]
  println "rawResponse: ${rawResponse}"

  return "${rawResponse}"
}

//Goal: execute a SH command in a thread to avoid hang when using build params
def executeSh(String pipedCommand){
  dir("${WORKSPACE}"){
    echo "executeSh()"
    echo pwd()
    def process = [ 'bash', '-c', "${pipedCommand}" ].execute()
    def out = new ByteArrayOutputStream()
    def err = new ByteArrayOutputStream()
    process.consumeProcessOutput(out, err)
    process.waitFor()
    //def output = process.text
    echo "Output is: ${out.toString()}"
    echo "Error is: ${err.toString()}"
    return out.toString()
    //return "${output}"
  }
}
