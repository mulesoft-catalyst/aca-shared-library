
# Automated Canary Analysis Jenkins Shared Library
A Jenkins shared library to perform automated canary analysis (ACA) with Mule 4 using Groovy, configuring a canary release policy, generating interesting traffic for metrics gathering, executing the analysis and acting based on the results. Using this shared library you would be able to:
  - Extend your existing pipelines to perform ACA
  - Automate the configuration in Anypoint Platform with the required custom components
  - Create a Jenkins HTML report with the results of the Load Testing executed to generate data points for the ACA

### Why?
A canary release helps organizations to reduce the risks of introducing new versions of a software by incrementally rolling out traffic to the new version, improving the observability and limiting the impact of the new components over the existing service. An automated canary analysis is the practice of understanding if a canary version is suitable to be considered a production ready solution, based on a set of rules, thresholds or decisions pre-configured.

### How?
This solution was specifically designed to be executed after the deployment of a canary in production and is recommended for those cases where both versions of the API (base and canary) can be tested with a representative endpoint. For example a GET on a resource.
For the solution to work, a stack is required to run the canary analyzes. The stack is made up of:
-   Prometheus: Prometheus is a monitoring system that includes its own time series database. It works by scraping endpoints that we specify, to collect the metrics.

-   Kayenta: Kayenta is an open source tool developed by Netflix that performs an automated canary analysis. It collects a list of metrics produced by the canary and the previous version (both stored in a metrics source), and compares them using statistical methods to obtain an overall result to determine if there are anomalies or problems, indicating the rollout should be aborted if the new service fails to meet specified tolerances. It also has a REST API that makes it convenient to integrate this analysis in CI/CD environments.

	NOTE: Generally, Kayenta comes installed as a Spinnaker module (a CI tool), but since we are only interested in the ACA part, only the Kayenta standalone installation is needed.

-   Redis: In memory data structure store, used by Kayenta to keep track of the internal pipelines run by Kayenta.
NOTE: Kayenta requires redis to work. Redis must work in a non-clustered fashion.

-  Metrics API: a custom Mule API used to collect metrics from a key-value store. This store by default is an Object Store, but could be anything else, like Redis. Basically this API is the main point of contact for Prometheus to scrape the metrics.

- Canary Policy: this is a custom policy made to balance the traffic to different APIs (base and canary) based on weights captured as percentages. For instance, you could configure the base version to receive 90% of the traffic, while leaving the canary with the other 10%

### Shared Library Architecture
The shared library is made up of 4 Groovy scripts:
- acaBuildParams.groovy: this script contains the parameters that the pipeline steps require to work
- acaPipeline.groovy: this script defines the pipeline steps to perform an ACA
- acaJobs.groovy: this script contains the jobs (functions) with the logic implementation. acaPipeline uses this one
- commons.groovy: this script contains repetitive functions and is called by acaJobs.groovy

### Pre-requisites
- Jenkins and a Jenkins Secret called "connected-app-credentials" (you can rename it) containing the credentials of a [connected app](https://docs.mulesoft.com/access-management/connected-apps-overview)
- [Canary Policy for Mule 4](https://github.com/mulesoft-catalyst/canary-policy-mule-4)
- [Kayenta](https://github.com/spinnaker/kayenta/blob/master/docs/kayenta-standalone.md) (and its dependencies)
- Prometheus
- Metrics API (Not provided)

### Usage
1. Import the shared library in Jenkins (in Jenkins go to: *manage jenkins* -> *configure system* -> *Global Pipeline Libraries*)
2. In your jenkinsfile add the following
```
@Library('automated-canary-analysis-lib') _ //import the library. The name used here must be equal to the given during step 1
...
acaBuildParams() //reference and configure the build params required to run the ACA Pipeline
acaPipeline() //Run the ACA pipeline
...
```

### Limitations
- Some Groovy functions are implemented without reusing some of the existing functions defined in commons.groovy. This is mainly due to some [Serialization](https://stackoverflow.com/questions/37864542/jenkins-pipeline-notserializableexception-groovy-json-internal-lazymap) errors experienced in Groovy. This has turned the code a little bit DRY. Whoever uses this library can choose to make a general refactor, which has not been done yet since it is advisable to replace some native Groovy libraries with software that does not come by default in Jenkins (e.g. jq to replace JSON slurper and sed).
- Current pipeline is based on CloudHub. Using this with other MuleSoft deployment models requires extending the functionality to include the different Platform APIs.
- Current Canary Config is embedded in the pipeline. The only parameterization that supports is the start and end time of the analysis. It is necessary to modify the metrics query to adapt it to the specific needs of those who implement it (name of aggregated metrics, aggregation functions, etc). It is recommended to externalize this configuration either as one more parameter or as an external dynamic file.


### Contribution

Want to contribute? Great!

* For public contributions - Just fork the repo, make your updates and open a pull request!
* For internal contributions - Use a simplified feature workflow following these steps:
   - Clone the repo
   - Create a feature branch using the naming convention feature/name-of-the-feature
   - Once it's ready, push your changes
   - Open a pull request for a review
