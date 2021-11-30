#!/usr/bin/groovy

//TODO: Externalize into a separate shared library. Repurpose to use executePOSTBash
def getAuthToken() {
  def oAuthUrl = "https://anypoint.mulesoft.com/accounts/api/v2/oauth2/token"
  def clientId = "1726d936b1d14b1f9a23282f0e5a7330" //TODO: externalize into credentials
  def clientSecret = "5B02329f8D264ec9822fFc344BFd405f" //TODO: externalize into credentials
  return sh (script: "curl \
    -s ${oAuthUrl} \
    -X POST \
    -H 'Content-Type: application/json' \
    -d '{\"grant_type\": \"client_credentials\", \"client_id\": \"${clientId}\", \"client_secret\": \"${clientSecret}\"}' \
    | sed -n 's|.*\"access_token\":\"\\([^\"]*\\)\".*|\\1|p'", returnStdout: true).trim()
}
