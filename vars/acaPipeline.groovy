#!/usr/bin/groovy

def analysisId = ''


//Waiting time in seconds to sleep before retrieve analysis result. TODO: Externalize as part of the canary configuration
def call(Map config){
  pipeline {
      agent any

      stages{
        stage("Apply Canary Policy"){
          steps {
            script {
              echo "Using map: Calling applyCanaryPolicy with ${config.organizationId}, ${config.environmentId}, ${config.groupId}, ${config.assetId}, ${config.assetName}, ${config.assetVersion}, ${config.assetClassifier}, ${config.apiVersion}, ${config.assetIdPolicy}, ${config.assetVersionPolicy}, ${config.host}, ${config.port}, ${config.protocol}, ${config.path}, ${config.weight}, ${config.hostCanary}, ${config.portCanary}, ${config.protocolCanary}, ${config.pathCanary}, ${config.weightCanary}"
              acaJobs.applyCanaryPolicy("${config.organizationId}", "${config.environmentId}", "${config.groupId}", "${config.assetId}", "${config.assetName}", "${config.assetVersion}", "${config.assetClassifier}", "${config.apiVersion}", "${config.assetIdPolicy}", "${config.assetVersionPolicy}", "${config.host}", "${config.port}", "${config.protocol}", "${config.path}", "${config.weight}", "${config.hostCanary}", "${config.portCanary}", "${config.protocolCanary}", "${config.pathCanary}", "${config.weightCanary}")
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
                script {
                  acaJobs.executeLoadTesting("${NEWMAN_PATH}", "${NEWMAN_COLLECTION}", "${NEWMAN_ITERATIONS}", "${POSTMAN_REPORT_PATH}", "${POSTMAN_REPORT_FILENAME}")
                }
            }
          }

          stage("Execute Canary Analysis"){
            environment{
              //Canary Analysis config
              canaryConfig='{\"canaryConfig\":{\"name\":\"canary-config-prometheus\",\"description\":\"Configuration for Prometheus\",\"configVersion\":\"1\",\"applications\":[\"ad-hoc\"],\"judge\":{\"name\":\"NetflixACAJudge-v1.0\",\"judgeConfigurations\":{}},\"metrics\":[{\"name\":\"Avg Response Time\",\"query\":{\"type\":\"prometheus\",\"customInlineTemplate\":\"PromQL:avg(avg_over_time(cloudhub_prometheus_rt{instance=\\\"prometheus-metrics.us-e2.cloudhub.io:80\\\", job=\\\"cloudhub-metrics\\\", canary=\\\"${scope}\\\"}[120m]))\",\"serviceType\":\"prometheus\"},\"groups\":[\"Canaries\"],\"analysisConfigurations\":{\"canary\":{\"critical\":false,\"nanStrategy\":\"replace\",\"effectSize\":{\"allowedIncrease\":1,\"allowedDecrease\":1},\"outliers\":{\"strategy\":\"keep\"},\"direction\":\"decrease\"}},\"scopeName\":\"default\"},{\"name\":\"Success Rate\",\"query\":{\"type\":\"prometheus\",\"customInlineTemplate\":\"PromQL:sum(sum_over_time(cloudhub_prometheus{instance=\\\"prometheus-metrics.us-e2.cloudhub.io:80\\\", job=\\\"cloudhub-metrics\\\", http_code=\\\"200\\\", canary=\\\"${scope}\\\"}[120m]))\",\"serviceType\":\"prometheus\"},\"groups\":[\"Canaries\"],\"analysisConfigurations\":{\"canary\":{\"nanStrategy\":\"replace\",\"critical\":false,\"effectSize\":{\"allowedIncrease\":1,\"allowedDecrease\":1},\"outliers\":{\"strategy\":\"keep\"},\"direction\":\"decrease\"}},\"scopeName\":\"default\"}],\"templates\":{},\"classifier\":{\"groupWeights\":{\"Canaries\":100}}},\"executionRequest\":{\"scopes\":[{\"scopeName\":\"default\",\"controlScope\":0,\"controlLocation\":\"us-east-1\",\"controlOffsetInMinutes\":\"10\",\"experimentScope\":1,\"experimentLocation\":\"us-east-1\",\"startTimeIso\":\"2021-11-17T15:00:00Z\",\"endTimeIso\":\"2021-11-17T19:00:00Z\",\"step\":2,\"extendedScopeParams\":{}}],\"thresholds\":{\"pass\":95,\"marginal\":75}}}'
            }
            steps {
              script {
                analysisId = acaJobs.executeCanaryAnalysis("${config.canaryServerProtocol}", "${config.canaryServer}", "${config.canaryServerPort}", "${canaryConfig}", "${config.assetId}")
                echo "Analysis ID: ${analysisId}"
              }
            }
          }

          stage("Wait period"){
            steps {
              sleep("${config.waitTime}")
            }
          }

          stage("Retrieve Analysis canary Results"){
            steps {
              script {
                def analysisresult = acaJobs.retrieveAnalysisResults("${config.canaryServerProtocol}", "${config.canaryServer}", "${config.canaryServerPort}", analysisId)
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
}
