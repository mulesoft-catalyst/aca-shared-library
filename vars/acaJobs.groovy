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
    def body = '{"canaryConfig":{"name":"canary-config-prometheus","description":"Configuration for Prometheus","configVersion":"1","applications":["ad-hoc"],"judge":{"name":"NetflixACAJudge-v1.0","judgeConfigurations":{}},"metrics":[{"name":"Avg Response Time","query":{"type":"prometheus","customInlineTemplate":"PromQL:avg(avg_over_time(cloudhub_prometheus_rt{instance=\"prometheus-metrics.us-e2.cloudhub.io:80\", job=\"cloudhub-metrics\", canary=\"${scope}\"}[120m]))","serviceType":"prometheus"},"groups":["Canaries"],"analysisConfigurations":{"canary":{"critical":false,"nanStrategy":"replace","effectSize":{"allowedIncrease":1,"allowedDecrease":1},"outliers":{"strategy":"keep"},"direction":"decrease"}},"scopeName":"default"},{"name":"Success Rate","query":{"type":"prometheus","customInlineTemplate":"PromQL:sum(sum_over_time(cloudhub_prometheus{instance=\"prometheus-metrics.us-e2.cloudhub.io:80\", job=\"cloudhub-metrics\", http_code=\"200\", canary=\"${scope}\"}[120m]))","serviceType":"prometheus"},"groups":["Canaries"],"analysisConfigurations":{"canary":{"nanStrategy":"replace","critical":false,"effectSize":{"allowedIncrease":1,"allowedDecrease":1},"outliers":{"strategy":"keep"},"direction":"decrease"}},"scopeName":"default"}],"templates":{},"classifier":{"groupWeights":{"Canaries":100}}},"executionRequest":{"scopes":[{"scopeName":"default","controlScope":0,"controlLocation":"us-east-1","controlOffsetInMinutes":"10","experimentScope":1,"experimentLocation":"us-east-1","startTimeIso":"2021-11-17T15:00:00Z","endTimeIso":"2021-11-17T19:00:00Z","step":2,"extendedScopeParams":{}}],"thresholds":{"pass":95,"marginal":75}}}'
    def http = new URL("http://192.168.0.45:8090/standalone_canary_analysis/?metricsAccountName=canary-prometheus&storageAccountName=in-memory-store-account&application=Canary+Test").openConnection() as HttpURLConnection
    http.setRequestMethod('POST')
    http.setDoOutput(true)
    http.setRequestProperty("Accept", '*/*')
    http.setRequestProperty("Content-Type", 'application/json')

    http.outputStream.write(body.getBytes("UTF-8"))
    http.connect()

    def response = [:]

    if (http.responseCode == 200) {
        println "HTTP response code is 200"
        response = new JsonSlurper().parseText(http.inputStream.getText('UTF-8'))
    } else {
        println "HTTP response code is different than 200"
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
