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
  def post = new URL("http://192.168.0.45:8090/standalone_canary_analysis/?metricsAccountName=canary-prometheus&storageAccountName=in-memory-store-account&application=Canary+Test").openConnection();
  def message = '"{\n  \"canaryConfig\": {\n    \"name\": \"canary-config-prometheus\",\n    \"description\": \"Configuration for Prometheus\",\n    \"configVersion\": \"1\",\n    \"applications\": [\n      \"ad-hoc\"\n    ],\n    \"judge\": {\n      \"name\": \"NetflixACAJudge-v1.0\",\n      \"judgeConfigurations\": {}\n    },\n    \"metrics\": [\n      {\n        \"name\": \"Avg Response Time\",\n        \"query\": {\n          \"type\": \"prometheus\",\n          \"customInlineTemplate\": \"PromQL:avg(avg_over_time(cloudhub_prometheus_rt{instance=\\\"prometheus-metrics.us-e2.cloudhub.io:80\\\", job=\\\"cloudhub-metrics\\\", canary=\\\"${scope}\\\"}[120m]))\",\n          \"serviceType\": \"prometheus\"\n        },\n        \"groups\": [\n          \"Canaries\"\n        ],\n        \"analysisConfigurations\": {\n          \"canary\": {\n            \"critical\": false,\n            \"nanStrategy\": \"replace\",\n            \"effectSize\": {\n              \"allowedIncrease\": 1,\n              \"allowedDecrease\": 1\n            },\n            \"outliers\": {\n              \"strategy\": \"keep\"\n            },\n            \"direction\": \"decrease\"\n          }\n        },\n        \"scopeName\": \"default\"\n      },\n      {\n        \"name\": \"Success Rate\",\n        \"query\": {\n          \"type\": \"prometheus\",\n          \"customInlineTemplate\": \"PromQL:sum(sum_over_time(cloudhub_prometheus{instance=\\\"prometheus-metrics.us-e2.cloudhub.io:80\\\", job=\\\"cloudhub-metrics\\\", http_code=\\\"200\\\", canary=\\\"${scope}\\\"}[120m]))\",\n          \"serviceType\": \"prometheus\"\n        },\n        \"groups\": [\n          \"Canaries\"\n        ],\n        \"analysisConfigurations\": {\n          \"canary\": {\n            \"nanStrategy\": \"replace\",\n            \"critical\": false,\n            \"effectSize\": {\n              \"allowedIncrease\": 1,\n              \"allowedDecrease\": 1\n            },\n            \"outliers\": {\n              \"strategy\": \"keep\"\n            },\n            \"direction\": \"decrease\"\n          }\n        },\n        \"scopeName\": \"default\"\n      }\n    ],\n    \"templates\": {},\n    \"classifier\": {\n      \"groupWeights\": {\n        \"Canaries\": 100\n      }\n    }\n  },\n  \"executionRequest\": {\n    \"scopes\": [\n    {\n        \"scopeName\": \"default\",\n        \"controlScope\": 0,\n        \"controlLocation\": \"us-east-1\",\n        \"controlOffsetInMinutes\": \"10\",\n        \"experimentScope\": 1,\n        \"experimentLocation\": \"us-east-1\",   \n        \"startTimeIso\": \"2021-11-17T15:00:00Z\",\n        \"endTimeIso\": \"2021-11-17T19:00:00Z\",\n        \"step\": 2,\n        \"extendedScopeParams\": {}\n    }\n    ],\n    \"thresholds\": {\n      \"pass\": 95,\n      \"marginal\": 75\n    }\n  }\n}"'
  post.setRequestMethod("POST")
  post.setDoOutput(true)
  post.setRequestProperty("Accept", '*/*')
  post.setRequestProperty("Content-Type", "application/json")
  post.getOutputStream().write(message.getBytes("UTF-8"));
  def postRC = post.getResponseCode();
  println(postRC);
  if(postRC.equals(200)) {
    println(post.getInputStream().getText());
  }
}

def retrieveAnalysisResults(){
  echo "ok"
}

def decideBasedOnResults(){
  echo "ok"
}
