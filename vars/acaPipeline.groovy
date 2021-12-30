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
def analysisResult = ''
def applyCanaryPolicyResponse = ''
def proxyApiId = ''
def policyId = ''
def appId = ''

def call(Map config){
  pipeline {
      agent any

      stages{
        stage("Obtain AP Token"){
          /*when {
              branch 'nonexistant'
          }*/
          steps {
            script {
              acaJobs.obtainAPToken()
            }
          }
        }
        stage("Apply Canary Policy"){
          /*when {
              branch 'nonexistant'
          }*/
          steps {
            script {
              echo "Using map: Calling applyCanaryPolicy with ${params.organizationId}, ${params.environmentId}, ${params.groupId}, ${params.assetId}, ${params.assetName}, ${params.assetVersion}, ${params.assetClassifier}, ${params.apiVersion}, ${params.assetIdPolicy}, ${params.assetVersionPolicy}, ${params.host}, ${params.port}, ${params.protocol}, ${params.path}, ${params.weight}, ${params.hostCanary}, ${params.portCanary}, ${params.protocolCanary}, ${params.pathCanary}, ${params.weightCanary}"
               applyCanaryPolicyResponse = acaJobs.applyCanaryPolicy("${params.organizationId}", "${params.environmentId}", "${params.groupId}", "${params.assetId}", "${params.assetName}", "${params.assetVersion}", "${params.assetClassifier}", "${params.apiVersion}", "${params.assetIdPolicy}", "${params.assetVersionPolicy}", "${params.host}", "${params.port}", "${params.protocol}", "${params.path}", "${params.weight}", "${params.hostCanary}", "${params.portCanary}", "${params.protocolCanary}", "${params.pathCanary}", "${params.weightCanary}")
               proxyApiId = applyCanaryPolicyResponse[0]
               policyId = applyCanaryPolicyResponse[1]
               appId = applyCanaryPolicyResponse[2]
            }
          }
        }

        stage("Wait for deployment"){
          /*when {
              branch 'nonexistant'
          }*/
          steps {
            sleep("${params.deploymentWaitTime}")
          }
        }

        stage('Canary Load tests') {
            /*when {
                branch 'nonexistant'
            }*/
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
            steps {
              script {
                analysisId = acaJobs.executeCanaryAnalysis("${params.canaryServerProtocol}", "${params.canaryServer}", "${params.canaryServerPort}", "${params.assetId}")
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
                analysisResult = acaJobs.retrieveAnalysisResults("${params.canaryServerProtocol}", "${params.canaryServer}", "${params.canaryServerPort}", analysisId)
              }
            }
          }

          stage("Decide Based on Results"){
            /*when {
                branch 'nonexistant'
            }*/
            steps {
              script {
                acaJobs.decideBasedOnResults("${analysisResult}", "${proxyApiId}", "${policyId}")
              }
            }
          }
      }
  }
}
