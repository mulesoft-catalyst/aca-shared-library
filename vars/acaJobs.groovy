#!/usr/bin/groovy
import groovy.json.JsonSlurper

def printMessage(message){
  echo "${message}"
}

def applyCanaryPolicy(){
  echo "ok"
}

def executeLoadTesting(){
  echo "ok"
}

def executeCanaryAnalysis(){
  try {
    def body = '{"id": 120}'
    def http = new URL("http://localhost:8090/standalone_canary_analysis/?metricsAccountName=canary-prometheus&storageAccountName=in-memory-store-account&application=Canary+Test").openConnection() as HttpURLConnection
    http.setRequestMethod('POST')
    http.setDoOutput(true)
    http.setRequestProperty("Accept", '*/*')
    http.setRequestProperty("Content-Type", 'application/json')

    http.outputStream.write(body.getBytes("UTF-8"))
    http.connect()

    def response = [:]

    if (http.responseCode == 200) {
        response = new JsonSlurper().parseText(http.inputStream.getText('UTF-8'))
    } else {
        response = new JsonSlurper().parseText(http.errorStream.getText('UTF-8'))
    }

    println "response: ${response}"

  } catch (Exception e) {
    echo e
  }
}

def retrieveAnalysisResults(){
  echo "ok"
}

def decideBasedOnResults(){
  echo "ok"
}
