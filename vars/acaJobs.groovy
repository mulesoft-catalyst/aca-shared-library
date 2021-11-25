#!/usr/bin/groovy

def call(String name = 'User') {
 echo "Welcome, ${name}."
}

def printMessage(message){
  echo ${message}
}
