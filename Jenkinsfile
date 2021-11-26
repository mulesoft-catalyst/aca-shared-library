@Library('automated-canary-analysis-lib') _

def analysisId = ''
def analysisresult = ''

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
            POSTMAN_REPORT_PATH = "target"
          }
          steps {
              sh """ ${NEWMAN_PATH} run ${NEWMAN_COLLECTION} \
                -n ${NEWMAN_ITERATIONS} \
                -r htmlextra \
                --reporter-htmlextra-export ${POSTMAN_REPORT_PATH} \
                --suppress-exit-code """


            script {
              LOAD_TEST_REPORT_COMPLETE_PATH = sh(script: "find ${POSTMAN_REPORT_PATH} -maxdepth 1 -name '*.html'", returnStdout: true).trim()
              LOAD_TEST_REPORT_NAME = sh(script: "basename ${LOAD_TEST_REPORT_COMPLETE_PATH}", returnStdout: true).trim()
            }

            publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: false, reportDir: "${POSTMAN_REPORT_PATH}", reportFiles: "${LOAD_TEST_REPORT_NAME}", reportName: 'Integration Test Report', reportTitles: ''])
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
              echo "Result is: ${analysisresult}"
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
