#!/usr/bin/groovy

//Author: Gaston Panizza
//Date: November 2021
//Description: Common functions definition used across the Automated Canary Analysis Library. Can be used as base for other libraries as well
//Pre-requisites: create a Jenkins credentials called connected-app-credentials, where clientId is stored in the username field and clientSecret in the password field
//This script was created for research purposes. By no means should be used as-is without understanding what it does and the risks involved.
//Likewise, future modifications must be made by whoever uses it

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

         def response = executeSh(curlCommand)
         def rawResponse = response.split("HTTPSTATUS:")[0]
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

//Goal: execute a PATCH request with a body using Curl in a thread. TODO: refactor along with executePostWithBody to make it a single function
def executePatchWithBody(String url, String body, String token){
  String curlCommand="curl -X PATCH -d '${body}' -w 'HTTPSTATUS:%{http_code}' -H \"Content-Type: application/json\" -H \"Authorization: Bearer ${token}\" ${url}"
  //TODO: refactor. Calling executeSh will throw a serialization exception given the nested LazyMaps
  def process = [ 'bash', '-c', "${curlCommand}" ].execute()
  def out = new ByteArrayOutputStream()
  def err = new ByteArrayOutputStream()
  process.consumeProcessOutput(out, err)
  process.waitFor()
  String response = out.toString().trim()
  process = null
  err = null
  out = null
  return response
}

//Goal: execute a multipart POST request using Curl in a thread
def executePostWithMultipart(String curlCommand, String expectedHttpCode, String methodName){
  def response = executeSh(curlCommand)

  def rawResponse = response.split("HTTPSTATUS:")[0]
  println "rawResponse: ${rawResponse}"

  return "${rawResponse}"
}


//Goal: execute a DELETE request with a body using Curl in a thread.
def executeDelete(String curlCommand, String expectedHttpCode, String methodName){
  //TODO: refactor. Calling executeSh will throw a serialization exception given the nested LazyMaps
  def process = [ 'bash', '-c', "${curlCommand}" ].execute()
  def out = new ByteArrayOutputStream()
  def err = new ByteArrayOutputStream()
  process.consumeProcessOutput(out, err)
  process.waitFor()
  String response = out.toString().trim()
  process = null
  err = null
  out = null
  return response
}

//Goal: execute a SH command in a thread to avoid hang when using build params
def executeSh(String pipedCommand){
  dir("${WORKSPACE}"){
    def process = [ 'bash', '-c', "${pipedCommand}" ].execute()
    def out = new ByteArrayOutputStream()
    def err = new ByteArrayOutputStream()
    process.consumeProcessOutput(out, err)
    process.waitFor()
    println "Output is: ${out.toString()}"
    println "Error is: ${err.toString()}"
    String response = out.toString()
    process = null
    err = null
    out = null
    return response
  }
}
