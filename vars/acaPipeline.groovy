#!/usr/bin/groovy

//Author: Gaston Panizza
//Date: November 2021
//Description: Pipeline definition to execute an automated canary analysis.
//Usage: acaBuildParams() reference and configure the build params require to run the Pipeline
//       acaPipeline() Run the pipeline
//Pre-requisites: This pipeline uses the following:
// Newman and HTMLExtra (it uses CSS to beautify reports so may be needed to allow css execution on Jenkins nodes -System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")-)
//This script was created for research purposes. By no means should be used as-is without understanding what it does and the risks involved.
//Likewise, future modifications must be made by whoever uses it

def analysisId = ''

//Waiting time in seconds to sleep before retrieve analysis result. TODO: Externalize as part of the canary configuration
def call(Map config){
  pipeline {
      agent any

      stages{
        stage("Apply Canary Policy"){
          steps {
            script {
              echo "Using map: Calling applyCanaryPolicy with ${params.organizationId}, ${params.environmentId}, ${params.groupId}, ${params.assetId}, ${params.assetName}, ${params.assetVersion}, ${params.assetClassifier}, ${params.apiVersion}, ${params.assetIdPolicy}, ${params.assetVersionPolicy}, ${params.host}, ${params.port}, ${params.protocol}, ${params.path}, ${params.weight}, ${params.hostCanary}, ${params.portCanary}, ${params.protocolCanary}, ${params.pathCanary}, ${params.weightCanary}"
              acaJobs.applyCanaryPolicy("${params.organizationId}", "${params.environmentId}", "${params.groupId}", "${params.assetId}", "${params.assetName}", "${params.assetVersion}", "${params.assetClassifier}", "${params.apiVersion}", "${params.assetIdPolicy}", "${params.assetVersionPolicy}", "${params.host}", "${params.port}", "${params.protocol}", "${params.path}", "${params.weight}", "${params.hostCanary}", "${params.portCanary}", "${params.protocolCanary}", "${params.pathCanary}", "${params.weightCanary}")
            }
          }
        }

        stage("Wait for deployment"){
          steps {
            sleep("${params.deploymentWaitTime}")
          }
        }

        stage('Canary Load tests') {
            environment {
              NEWMAN_PATH = "newman"
              NEWMAN_COLLECTION = "base_collection.json"
              NEWMAN_ITERATIONS = 50
              POSTMAN_REPORT_PATH = "/"
              POSTMAN_REPORT_FILENAME = "index.html"
            }
            steps {
                script {
                  acaJobs.executeLoadTesting("${NEWMAN_PATH}", "${WORKSPACE}/${NEWMAN_COLLECTION}", "${NEWMAN_ITERATIONS}", "${WORKSPACE}${POSTMAN_REPORT_PATH}", "${POSTMAN_REPORT_FILENAME}")
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
                analysisId = acaJobs.executeCanaryAnalysis("${params.canaryServerProtocol}", "${params.canaryServer}", "${params.canaryServerPort}", "${canaryConfig}", "${params.assetId}")
                echo "Analysis ID: ${analysisId}"
              }
            }
          }

          stage("Wait period"){
            steps {
              sleep("${params.waitTime}")
            }
          }

          stage("Retrieve Analysis canary Results"){
            steps {
              script {
                def analysisresult = acaJobs.retrieveAnalysisResults("${params.canaryServerProtocol}", "${params.canaryServer}", "${params.canaryServerPort}", analysisId)
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
