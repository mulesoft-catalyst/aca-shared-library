@Library('automated-canary-analysis-lib') _

pipeline {
    agent any

    stages{
      stage("Apply Canary Policy"){
        steps {
          acaJobs.applyCanaryPolicy()
        }
      }

      stage('Canary Load tests') {
          environment {
            NEWMAN_PATH = "newman"
            NEWMAN_COLLECTION = "newman-example-collection.postman_collection.json"
            NEWMAN_ITERATIONS = 50
          }
          steps {
              sh """ ${NEWMAN_PATH} run ${NEWMAN_COLLECTION} -n ${NEWMAN_ITERATIONS} """
          }
        }

        stage("Execute Canary Analysis"){
          steps {
            def analysisId = acaJobs.executeCanaryAnalysis()
            echo "${analysisId}"
          }
        }

        stage("Wait period"){
          steps {
            sleep(60)
          }
        }

        stage("Retrieve Analysis canary Results"){
          steps {
            def analysisresult = acaJobs.retrieveAnalysisResults(analysisId)
            echo "Result is: ${analysisresult}"
          }
        }

        stage("Decided Based on Results"){
          steps {
            acaJobs.decideBasedOnResults()
          }
        }
    }
}
