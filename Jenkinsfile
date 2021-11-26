//@Library('automated-canary-analysis-lib') _
//acaJobs.applyCanaryPolicy()
//acaJobs.executeLoadTesting()
//def analysisId = acaJobs.executeCanaryAnalysis()
//echo "${analysisId}"
//sleep(60)
//def analysisresult = acaJobs.retrieveAnalysisResults(analysisId)
//echo "Result is: ${analysisresult}"
//acaJobs.decideBasedOnResults()
//sh newman run "https://www.getpostman.com/collections/631643-f695cab7-6878-eb55-7943-ad88e1ccfd65-JsLv"

pipeline {
    agent any

    stages{

    stage('Integration tests') {

        environment {
          NEWMAN_PATH = "newman"

        }
        steps {
            sh """ ${NEWMAN_PATH} run newman-example-collection.postman_collection.json """
        }
      }

    }
}
