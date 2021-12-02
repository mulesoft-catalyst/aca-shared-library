#!/usr/bin/groovy


def call() {
    properties([
        parameters([
          string(name: 'organizationId', defaultValue: '9033ff23-884a-4352-b75b-14fc8237b2c4', description: 'The organization ID from Anypoint Platform'),
          string(name: 'environmentId', defaultValue: 'eb473ffd-2134-4ecf-b7bc-63a5d0856743', description: 'The environment ID from Anypoint Platform'),
          string(name: 'groupId', defaultValue: '9033ff23-884a-4352-b75b-14fc8237b2c4', description: 'The environment ID from Anypoint Platform'),
          string(name: 'assetIdPolicy', defaultValue: 'canary-release-mule4', description: 'The name given to the canary policy when installed'),
          string(name: 'assetVersionPolicy', defaultValue: '3.0.11-SNAPSHOT', description: 'The version of the canary policy to use'),
          string(name: 'host', defaultValue: 'httpstat.us', description: 'The target host for the baseline'),
          string(name: 'port', defaultValue: '443', description: 'The target port for the baseline'),
          string(name: 'protocol', defaultValue: 'HTTPS', description: 'The target protocol for the baseline'),
          string(name: 'path', defaultValue: '/200', description: 'The target protocol for the baseline'),
          string(name: 'weight', defaultValue: '50', description: 'The target weight for the baseline'),
          string(name: 'hostCanary', defaultValue: 'httpstat.us', description: 'The target host for the canary'),
          string(name: 'portCanary', defaultValue: '443', description: 'The target port for the canary'),
          string(name: 'protocolCanary', defaultValue: 'HTTPS', description: 'The target protocol for the canary'),
          string(name: 'pathCanary', defaultValue: '/200', description: 'The target protocol for the canary'),
          string(name: 'weightCanary', defaultValue: '50', description: 'The target weight for the canary'),
          string(name: 'assetName', defaultValue: 'canary-release-prx-REPLACEME', description: 'The name of the proxy to upload to Exchange'),
          string(name: 'assetId', defaultValue: 'canary-release-prx-REPLACEME', description: 'The id of the proxy to upload to Exchange'),
          string(name: 'assetClassifier', defaultValue: 'http', description: 'The type of asset to upload to Exchange'),
          string(name: 'apiVersion', defaultValue: 'v1', description: 'The version of the api to upload to Exchange'),
          string(name: 'assetVersion', defaultValue: '1.0.0', description: 'The version of the asset to upload to Exchange')
        ])
    ])
}
