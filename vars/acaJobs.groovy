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
  def post = new URL("https://httpbin.org/post").openConnection();
  def message = '{"message":"this is a message"}'
  post.setRequestMethod("POST")
  post.setDoOutput(true)
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
