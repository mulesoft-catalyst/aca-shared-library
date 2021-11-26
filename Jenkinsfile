@Library('automated-canary-analysis-lib') _
import groovy.json.*

def analysisId = ''
def analysisresult = null

pipeline {
    agent any

    stages{
      stage("Apply Canary Policy"){
        steps {
          script {
            acaJobs.applyCanaryPolicy()
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
              analysisresult = acaJobs.retrieveAnalysisResults(analysisId)
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
