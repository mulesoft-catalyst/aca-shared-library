#!/usr/bin/groovy


def call() {
    properties([
        parameters([
          string(name: 'organizationId', defaultValue: '9133ef23-884a-4352-b75b-14fc8237b2c2', description: 'The organization ID from Anypoint Platform'),
          string(name: 'environmentId', defaultValue: 'ec473ffd-2134-4ecf-b7bb-63a5d0856741', description: 'The environment ID from Anypoint Platform'),
          string(name: 'groupId', defaultValue: '9132ff23-884b-4352-b75a-14fc8237b2c3', description: 'The environment ID from Anypoint Platform'),
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
          string(name: 'pathCanary', defaultValue: '/500', description: 'The target protocol for the canary'),
          string(name: 'weightCanary', defaultValue: '50', description: 'The target weight for the canary'),
          string(name: 'assetName', defaultValue: 'canary-release-prx-REPLACEME', description: 'The name of the proxy to upload to Exchange'),
          string(name: 'assetId', defaultValue: 'canary-release-prx-REPLACEME', description: 'The id of the proxy to upload to Exchange'),
          string(name: 'assetClassifier', defaultValue: 'http', description: 'The type of asset to upload to Exchange'),
          string(name: 'apiVersion', defaultValue: 'v1', description: 'The version of the api to upload to Exchange'),
          string(name: 'assetVersion', defaultValue: '1.0.0', description: 'The version of the asset to upload to Exchange'),
          string(name: 'canaryServer', defaultValue: '192.168.0.45', description: 'The Kayenta server used to run the analysis'),
          string(name: 'canaryServerProtocol', defaultValue: 'http', description: 'The protocol used to connect to the Kayenta server'),
          string(name: 'canaryServerPort', defaultValue: '8090', description: 'The port used to connect to the Kayenta server'),
          string(name: 'startTimeIso', defaultValue: '2021-12-08T15:00:00Z', description: 'Analysis Start Time in ISO format'),
          string(name: 'endTimeIso', defaultValue: '2021-12-08T19:00:00Z', description: 'Analysis End Time in ISO format'),
          string(name: 'waitTime', defaultValue: '10', description: 'Wait time (in seconds) to wait until the Canary Analysis is completed'),
          string(name: 'deploymentWaitTime', defaultValue: '120', description: 'Wait time (in seconds) to wait until the proxy application gets deployed'),
          string(name: 'endpointProtocol', defaultValue: 'https', description: 'The protocol to access the endpoint used during the Load Test phase'),
          string(name: 'endpointHost', defaultValue: 'httpbin.org', description: 'The host to access the endpoint used during the Load Test phase (including port if needed)'),
          string(name: 'endpointResource', defaultValue: 'get', description: 'The resource to access the endpoint used during the Load Test phase'),
          string(name: 'weightBaseSuccessful', defaultValue: '80', description: 'The final weight to assign to the baseline version after the Canary Analysis was successful and the Canary is considered a suitable version'),
          string(name: 'weightCanarySuccessful', defaultValue: '20', description: 'The final weight to assign to the canary version after the Canary Analysis was successful and the Canary is considered a suitable version')
        ])
    ])
}
