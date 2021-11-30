import groovy.json.JsonSlurper
@Library('automated-canary-analysis-lib') _

def analysisId = ''

//API Endpoints configuration (Base and Canary). TODO: Externalize as pipeline parameters
//def host="httpstat.us"
//def port="443"
//def protocol="HTTPS"
//def path="/200"
//def weight="50"
//def hostCanary="httpstat.us"
//def portCanary="443"
//def protocolCanary="HTTPS"
//def pathCanary="/500"
//def weightCanary="50"

//def organizationId = "9033ff23-884a-4352-b75b-14fc8237b2c4"
//def environmentId = "eb473ffd-2134-4ecf-b7bc-63a5d0856743"

//Variables for Canary policy version (TODO: should be externalized to parametrized pipeline). This is the configuration of the existing policy version
//def groupId="9033ff23-884a-4352-b75b-14fc8237b2c4"
//def assetIdPolicy="canary-release-mule4"
//def assetVersionPolicy="3.0.11-SNAPSHOT"

//Canary proxy Exchange Asset (TODO: should be externalized to parametrized pipeline). This is the configuration of the asset to upload to Exchange
//def timeStampMilis=System.currentTimeMillis()
//def assetName="canary-release-prx-" + timeStampMilis
//def assetId="canary-release-prx-" + timeStampMilis
//def assetClassifier="http"
//def apiVersion="v1"
//def assetVersion="1.0.0"

pipeline {
    agent any
    parameters {
      string(name: 'organizationId', defaultValue: '9033ff23-884a-4352-b75b-14fc8237b2c4', description: 'The environment ID from Anypoint Platform')
      string(name: 'environmentId', defaultValue: 'eb473ffd-2134-4ecf-b7bc-63a5d0856743', description: 'The environment ID from Anypoint Platform')
      string(name: 'groupId', defaultValue: '9033ff23-884a-4352-b75b-14fc8237b2c4', description: 'The environment ID from Anypoint Platform')
      string(name: 'assetIdPolicy', defaultValue: 'canary-release-mule4', description: 'The name given to the canary policy when installed')
      string(name: 'assetVersionPolicy', defaultValue: '3.0.11-SNAPSHOT', description: 'The version of the canary policy to use')
      string(name: 'host', defaultValue: 'httpstat.us', description: 'The target host for the baseline')
      string(name: 'port', defaultValue: '443', description: 'The target port for the baseline')
      string(name: 'protocol', defaultValue: 'HTTPS', description: 'The target protocol for the baseline')
      string(name: 'path', defaultValue: '/200', description: 'The target protocol for the baseline')
      string(name: 'weight', defaultValue: '50', description: 'The target weight for the baseline')
      string(name: 'hostCanary', defaultValue: 'httpstat.us', description: 'The target host for the canary')
      string(name: 'portCanary', defaultValue: '443', description: 'The target port for the canary')
      string(name: 'protocolCanary', defaultValue: 'HTTPS', description: 'The target protocol for the canary')
      string(name: 'pathCanary', defaultValue: '/200', description: 'The target protocol for the canary')
      string(name: 'weightCanary', defaultValue: '50', description: 'The target weight for the canary')
      string(name: 'assetName', defaultValue: 'canary-release-prx-REPLACEME', description: 'The name of the proxy to upload to Exchange')
      string(name: 'assetId', defaultValue: 'canary-release-prx-REPLACEME', description: 'The id of the proxy to upload to Exchange')
      string(name: 'assetClassifier', defaultValue: 'http', description: 'The type of asset to upload to Exchange')
      string(name: 'apiVersion', defaultValue: 'v1', description: 'The version of the api to upload to Exchange')
      string(name: 'assetVersion', defaultValue: '1.0.0', description: 'The version of the asset to upload to Exchange')
    }
    environment {
      organizationId = "${params.organizationId}"
      environmentId = "${params.environmentId}"
      groupId = "${params.groupId}"
      assetIdPolicy = "${params.assetIdPolicy}"
      assetVersionPolicy = "${params.assetVersionPolicy}"
      host="${params.host}"
      port="${params.port}"
      protocol="${params.protocol}"
      path="${params.path}"
      weight="${params.weight}"
      hostCanary="${params.hostCanary}"
      portCanary="${params.portCanary}"
      protocolCanary="${params.protocolCanary}"
      pathCanary="${params.pathCanary}"
      weightCanary="${params.weightCanary}"
    }

    stages{
      stage("Apply Canary Policy"){
        steps {
          script {
            echo "Calling applyCanaryPolicy with ${organizationId}, ${environmentId}, ${groupId}, ${assetId}, ${assetName}, ${assetVersion}, ${assetClassifier}, ${apiVersion}, ${assetIdPolicy}, ${assetVersionPolicy}, ${host}, ${port}, ${protocol}, ${path}, ${weight}, ${hostCanary}, ${portCanary}, ${protocolCanary}, ${pathCanary}, ${weightCanary}"
            acaJobs.applyCanaryPolicy("${organizationId}", "${environmentId}", "${groupId}", "${assetId}", "${assetName}", "${assetVersion}", "${assetClassifier}", "${apiVersion}", "${assetIdPolicy}", "${assetVersionPolicy}", "${host}", "${port}", "${protocol}", "${path}", "${weight}", "${hostCanary}", "${portCanary}", "${protocolCanary}", "${pathCanary}", "${weightCanary}")
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
