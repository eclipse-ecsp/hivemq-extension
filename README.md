<div align="center">
  <img src="./images/logo.png" width="300" height="150"/>
</div>

[![Maven Build & Sonar Analysis](https://github.com/eclipse-ecsp/hivemq-extension/actions/workflows/maven-build.yml/badge.svg)](https://github.com/eclipse-ecsp/hivemq-extension/actions/workflows/maven-build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eclipse-ecsp_hivemq-extension&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=eclipse-ecsp_hivemq-extension)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=eclipse-ecsp_hivemq-extension&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=eclipse-ecsp_hivemq-extension)
[![License Compliance](https://github.com/eclipse-ecsp/hivemq-extension/actions/workflows/licence-compliance.yaml/badge.svg)](https://github.com/eclipse-ecsp/hivemq-extension/actions/workflows/licence-compliance.yaml)
[![Latest Release](https://img.shields.io/github/v/release/eclipse-ecsp/hivemq-extension?sort=semver)](https://github.com/eclipse-ecsp/hivemq-extension/releases)


# HiveMQ Custom Extension
The HiveMQ custom extension provides functionality to authenticate the MQTT client, provides default permissions on configured topics, subscribe, publish, and so on. This extension supports HiveMQ 4.X.

The following HiveMQ interceptors are implemented:
* ClientLifecycleEventListener
* SimpleAuthenticator
* ClientInitializer
* SubscriptionAuthorizer
* PublishInboundInterceptor
* PubackInboundInterceptor
* PubackOutboundInterceptor
* PingReqInboundInterceptor
* ConnectInboundInterceptor
* UnsubscribeInboundInterceptor

The extension supports the following types of authentication:
* Username/password authentication
* JWT token authentication
* Certificate based authentication.
<br/>
We strongly recommend to read the [HiveMQ Extension Documentations](https://docs.hivemq.com/hivemq-extension/latest/extensions/) to grasp the core concepts of HiveMQ extension development.


# Table of Contents
* [Getting Started](#getting-started)
* [Usage](#usage)
* [How to contribute](#how-to-contribute)
* [Built with Dependencies](#built-with-dependencies)
* [Code of Conduct](#code-of-conduct)
* [Contributors](#contributors)
* [Security Contact Information](#security-contact-information)
* [Support](#support)
* [Troubleshooting](#troubleshooting)
* [License](#license)
* [Announcements](#announcements)


## Getting Started

The following instructions copy the project and gets it up and running on your local machine for development and testing purposes. 

### Prerequisites

1. Download latest hivemq broker from [here](https://github.com/hivemq/hivemq-community-edition).
2. Run HiveMQ 4.X on Java 11 or later. (The extension is built on Java 17.)
3. A Kafka Broker must be up and running in local.
4. The extension integrates with Redis. By default, Redis communication is disabled. If you enable it by setting <b>redis.enable=true</b>, make sure Redis is installed and running before starting

### Installation

The following is a step-by-step series of examples that gets a local development environment running.


<b>Step 1:</b> Clone this repository into a Java 17 maven project.<br/>
<b>Step 2:</b> Update the following properties or path to run code in the local machine.
		
```
hivemq broker path in pom.xml, path where you have download hivemq broker.
<hiveMQDir>/opt/hivemq-ce-2024.4/</hiveMQDir>

update kafka broker url to localhost or ipaddress where kafka is running
kafka.broker.url=localhost:9092

In PluginConfig.java update file path for properties to your local path
@PropertySource("file:<folderPath>//src//test//resources//hivemq-plugin-base.properties")

In PropertyLoader.java update folder path for hivemq-plugin-base.properties file
private static String folderPath = "<folderPath>//src//test//resources//";

provide full path for below property in hivemq-plugin-base.properties
jwt.publickey.path=<folderPath>/src/test/resources/wso_test_publickey.txt

```

<b>Step 3:</b> Run application using IDE - Create a run configuration
<br/>

```
Goals - clean package
profile - RunWithHiveMQ

```



### Coding style check configuration

[checkstyle.xml](./checkstyle.xml) is the coding standard to follow while writing new/updating existing code.

Checkstyle plugin [maven-checkstyle-plugin:3.2.1](https://maven.apache.org/plugins/maven-checkstyle-plugin/) is integrated in [pom.xml](./pom.xml) which runs in the validate phase and check goal of the maven lifecycle and fails the build if there are any checkstyle errors in the project.

To run checkstyle plugin explicitly, run the following command: mvn checkstyle:check


### Running the tests

There are JUnit test cases added for all the functions that can be run using any IDE. Right-click the project -> Run As -> JUnit Test
<br/><br/>
If you want to submit a bug fix or new feature, ensure that all tests pass.

```
mvn test
```

Or run a specific test

```
mvn test -Dtest="TheFirstUnitTest"
```

To run a method from within a test

```
mvn test -Dtest="TheSecondUnitTest#whenTestCase2_thenPrintTest2_1"
```


<br/><br/>
For manual testing, connect using any mqtt client. 
<br/><b>Server URI</b> - localhost</b>
<br/><b>Port</b> - 1883, TCP Ports can be configured in hivemq config.xml

```
<listeners>
    <tcp-listener>
        <port>1883</port>
        <bind-address>0.0.0.0</bind-address>
    </tcp-listener>
</listeners>

```

<br/><b>username</b> - haa_api(can be configured in hivemq-plugin-base.properties in whitelisted.users property) 
<br/><b>password</b> - can be configured in hivemq-plugin-base.properties in mqtt.user.password property

  
### Deployment

The component can be deployed as a Kubernetes pod by installing Device factory management charts.
Link: [Charts](../../../ecsp-helm-charts/tree/main/hivemq-extension-cluster)

## Usage

You can go through [HiveMQ Extension Documentations](https://docs.hivemq.com/hivemq-extension/latest/extensions/) to learn more about hivemq extensions.


## Built With Dependencies

* [Spring](https://spring.io/projects/spring-framework) - Web framework used for building the application.
* [Maven](https://maven.apache.org/) - Build tool used for dependency management.
* [SLF4J](http://www.slf4j.org/) - Logging facade providing abstraction for various logging frameworks.
* [Logback](http://logback.qos.ch/) - Concrete logging implementation used with SLF4J.
* [Junit](https://junit.org/) - Unit testing framework.
* [Hivemq SDK](https://mvnrepository.com/artifact/com.hivemq/hivemq-extension-sdk) - Hivemq sdk to build extension
* [Kafka](https://kafka.apache.org/) - Distributed event streaming platform used for building real-time data pipelines and streaming applications.
* [redis](https://redis.io/) - In-memory data store used for caching
* [prometheus](https://prometheus.io/) - Monitoring and Alerting toolkit for recording real-time metrics and generating alerts


## How to contribute

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details about contribution guidelines and the process for submitting pull requests to us.

## Code of Conduct

See [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) for details about our code of conduct and the process for submitting pull requests to us.


## Contributors

* The list of [contributors](../../graphs/contributors) who participated in this project.

## Security Contact Information

See [SECURITY.md](./SECURITY.md) to raise any security related issues.

## Support

* Contact the project developers via the project's "dev" list - [ecsp-dev](https://accounts.eclipse.org/mailing-list/)

## Troubleshooting

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details about raising an issue and submitting a pull request to us.

## License

This project is licensed under the Apache 2.0 License. See the [LICENSE](./LICENSE) file for details.

## Announcements

All updates to this component are documented in [releases page](https://github.com/eclipse-ecsp/hivemq-extension/releases).
For the available versions, see the [tags on this repository](https://github.com/eclipse-ecsp/hivemq-extension/tags).

