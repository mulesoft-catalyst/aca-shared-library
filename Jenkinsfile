@Library('automated-canary-analysis-lib') _
parameters([
      password(name: 'organizationId', defaultValue: '9033ff23-884a-4352-b75b-14fc8237b2c4', description: 'The organization ID from Anypoint Platform')
])

//TODO: Externalize as build pipeline parameters
acaPipeline(organizationId: '${params.organizationId}', //'9033ff23-884a-4352-b75b-14fc8237b2c4',
            environmentId: 'eb473ffd-2134-4ecf-b7bc-63a5d0856743',
            groupId: '9033ff23-884a-4352-b75b-14fc8237b2c4',
            assetIdPolicy: 'canary-release-mule4',
            assetVersionPolicy: '3.0.11-SNAPSHOT',
            host: 'httpstat.us',
            port: '443',
            protocol: 'HTTPS',
            path: '/200',
            weight: '50',
            hostCanary: 'httpstat.us',
            portCanary: '443',
            protocolCanary: 'HTTPS',
            pathCanary: '/500',
            weightCanary: '50',
            assetId: 'canary-release-prx-test-4',
            assetName: 'canary-release-prx-test-4',
            assetClassifier: 'http',
            apiVersion: 'v1',
            assetVersion: '1.0.0',
            canaryServer: '192.168.0.45',
            canaryServerProtocol: 'http',
            canaryServerPort: 8090,
            waitTime: 10)
