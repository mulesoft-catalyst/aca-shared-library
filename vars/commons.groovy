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
       return sh (script: "curl \
         -s ${oAuthUrl} \
         -X POST \
         -H 'Content-Type: application/json' \
         -d '{\"grant_type\": \"client_credentials\", \"client_id\": \"${clientId}\", \"client_secret\": \"${clientSecret}\"}' \
         | sed -n 's|.*\"access_token\":\"\\([^\"]*\\)\".*|\\1|p'", returnStdout: true).trim()
  }
}
