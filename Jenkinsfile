import groovy.json.JsonSlurper
@Library('automated-canary-analysis-lib') _

def analysisId = ''

//API Endpoints configuration (Base and Canary). TODO: Externalize as pipeline parameters
def host="httpstat.us"
def port="443"
def protocol="HTTPS"
def path="/200"
def weight="50"
def hostCanary="httpstat.us"
def portCanary="443"
def protocolCanary="HTTPS"
def pathCanary="/500"
def weightCanary="50"

def organizationId = "9033ff23-884a-4352-b75b-14fc8237b2c4"

//Variables for Canary policy version
def groupId="6824e136-3ef5-4c4d-aaac-438f3dc41ac2"
def assetIdPolicy="canary-release-mule4"
def assetVersion="1.0.0"

//Canary proxy Exchange Asset (should be externalized to parametrized pipeline)
def assetName="canary-release-prx-4"
def assetId="canary-release-prx-4"
def assetClassifier="http"
def apiVersion="v1"

pipeline {
    agent any

    stages{
      stage("Apply Canary Policy"){
        steps {
          script {
            echo "Calling applyCanaryPolicy with ${organizationId}, ${groupId}, ${assetId}, ${assetName}, ${assetVersion}, ${assetClassifier}, ${apiVersion}, ${host}, ${port}, ${protocol}, ${path}, ${weight}, ${hostCanary}, ${portCanary}, ${protocolCanary}, ${pathCanary}, ${weightCanary}"
            acaJobs.applyCanaryPolicy("${organizationId}", "${groupId}", "${assetId}", "${assetName}", "${assetVersion}", "${assetClassifier}", "${apiVersion}", "${host}", "${port}", "${protocol}", "${path}", "${weight}", "${hostCanary}", "${portCanary}", "${protocolCanary}", "${pathCanary}", "${weightCanary}")
          }
        }
      }

      stage('Canary Load tests') {
          environment {
            NEWMAN_PATH = "newman"
            NEWMAN_COLLECTION = "newman-example-collection.postman_collection.json"
            NEWMAN_ITERATIONS = 50
            POSTMAN_REPORT_PATH = "var/reports/newman/html"
            POSTMAN_REPORT_FILENAME = "index.html"
          }
          steps {
              sh """ ${NEWMAN_PATH} run ${NEWMAN_COLLECTION} \
                -n ${NEWMAN_ITERATIONS} \
                -r htmlextra \
                --reporter-htmlextra-export ${POSTMAN_REPORT_PATH}"/"${POSTMAN_REPORT_FILENAME} \
                --suppress-exit-code """


            publishHTML( target:
              [
                allowMissing: true,
                alwaysLinkToLastBuild: false,
                keepAll: false,
                reportDir: "${POSTMAN_REPORT_PATH}",
                reportFiles: "${POSTMAN_REPORT_FILENAME}",
                reportName: 'Canary Load Test Report',
                reportTitles: ''
              ]
            )
          }
        }

        stage("Execute Canary Analysis"){
          steps {
            script {
              analysisId = acaJobs.executeCanaryAnalysis()
              echo "${analysisId}"
            }
          }
        }

        stage("Wait period"){
          steps {
            sleep(10)
          }
        }

        stage("Retrieve Analysis canary Results"){
          steps {
            script {
              def analysisresult = acaJobs.retrieveAnalysisResults(analysisId)
            }
          }
        }

        stage("Decided Based on Results"){
          steps {
            script {
              acaJobs.decideBasedOnResults()
            }
          }
        }
    }
}
