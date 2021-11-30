#!/usr/bin/groovy

//Author: Gaston Panizza
//Date: November 2021
//Description: Common functions definition used across the Automated Canary Analysis Library. Can be used as base for other libraries as well
//This pipeline was created by the author specifically for AT&T needs,
//it is responsability of AT&T teams to evolve and maintain these scripts when
//the author is not involved in the project anymore

//Goal: Obtain an access token that can be used to authenticate Anypoint Platform APIs
def getAuthToken() {
  this.script.withCredentials([usernamePassword(credentialsId: "connected-app-credentials", passwordVariable: 'clientSecret', usernameVariable: 'clientId')]) {
       return this.script.sh (script: "curl \
         -s ${oAuthUrl} \
         -X POST \
         -H 'Content-Type: application/json' \
         -d '{\"grant_type\": \"client_credentials\", \"client_id\": \"${clientId}\", \"client_secret\": \"${clientSecret}\"}' \
         | sed -n 's|.*\"access_token\":\"\\([^\"]*\\)\".*|\\1|p'", returnStdout: true).trim()
  }
  //def oAuthUrl = "https://anypoint.mulesoft.com/accounts/api/v2/oauth2/token"
  //def clientId = "1726d936b1d14b1f9a23282f0e5a7330" //TODO: externalize into credentials
  //def clientSecret = "5B02329f8D264ec9822fFc344BFd405f" //TODO: externalize into credentials
  //return sh (script: "curl \
    //-s ${oAuthUrl} \
    //-X POST \
    //-H 'Content-Type: application/json' \
    //-d '{\"grant_type\": \"client_credentials\", \"client_id\": \"${clientId}\", \"client_secret\": \"${clientSecret}\"}' \
    //| sed -n 's|.*\"access_token\":\"\\([^\"]*\\)\".*|\\1|p'", returnStdout: true).trim()
}
