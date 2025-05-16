[<img src="./images/logo.png" width="300" height="150"/>](./images/logo.png)

[![Build Status](https://github.com/eclipse-ecsp/hivemq-extension/actions/workflows/maven.yml/badge.svg)](https://github.com/eclipse-ecsp/hivemq-extension/actions/workflows/maven.yml)
[![License](https://github.com/eclipse-ecsp/hivemq-extension/actions/workflows/license-compliance.yml/badge.svg)](https://github.com/eclipse-ecsp/hivemq-extension/actions/workflows/license-compliance.yml)


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
We strongly recommend to read the [HiveMQ Extension Documentation](https://docs.hivemq.com/hivemq/latest/extensions/) to grasp the core concepts of HiveMQ extension development.


# Table of Contents
* [Getting Started](#getting-started)
* [Usage](#usage)
* [How to contribute](#how-to-contribute)
* [Built with Dependencies](#built-with-dependencies)
* [Code of Conduct](#code-of-conduct)
* [Contributers](#contributors)
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
4. The extension uses Redis. By default, Redis communication is disabled but if you enable it using <b>redis.enable=true</b>, then you must download Redis before starting.


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

There is a file named **checkStyle.xml** in the project at the root level. Use it to configure checkstyle in your IDE. There are plugins available for all IDEs to run checkstyle.
<br/>
For Eclipse IDE, go to marketplace and install the eclipse-cs plugin.
<br/>

To set **checkStyle.xml** as your default checkstyle follow the following steps in Eclipse. 
<br/><br/>
**Windows -> Preferences -> Checkstyle -> New -> Type(select External COnfiguration File)**, Name - give any name, location - browse for checkStyle.xml -> OK
<br/><br/>
Select this newly added checkstyle and click on <b>Set as Default</b>
<br/>
<br/>
<b>You can include the checkstyle plugin in pom.xml to generate a checkstyle report.</b>
<br/>

[checkStyle.xml](./checkStyle.xml) is the HARMAN coding standard to follow while writing new code or updating existing code.

Checkstyle plugin [maven-checkstyle-plugin:3.2.1](https://maven.apache.org/plugins/maven-checkstyle-plugin/) is
integrated in [pom.xml](./pom.xml) which runs in the `validate` phase and `check` goal of the maven lifecycle. It fails
the build if there are any checkstyle errors in the project.


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

We can deploy this component as a Kubernetes pod by installing Device Message charts.
[Charts](https://github.com/eclipse-ecsp/csp-opensource-charts/tree/main/hivemq-cluster)

## Usage

You can go through [HiveMQ Extension Documentations](https://docs.hivemq.com/hivemq/latest/extensions/) to learn more about hivemq extensions.


## Built With Dependencies

* [Spring Boot](https://spring.io/projects/spring-boot/) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management
* [SLF4J](http://www.slf4j.org/) - Logging API
* [Logback](http://logback.qos.ch/) - Logging Framework
* ignite Libraries - Internal Harman libraries
* [Junit](https://junit.org/junit4/) - Test framework
* [Hivemq SDK](https://mvnrepository.com/artifact/com.hivemq/hivemq-extension-sdk) - Hivemq sdk to build extension
* [Kafka](https://kafka.apache.org/) - Apache Kafka 
* [redis](https://redis.io/) - Redis client
* [prometheus](https://prometheus.io/) - Monitoring and Alerting toolkit


## How to contribute

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details about contribution guidelines and the process for submitting pull requests to us.

## Code of Conduct

See [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) for details about our code of conduct and the process for submitting pull requests to us.


## Contributors

* For a list contributors to this project, see the [contributors](https://github.com/eclipse-ecsp/hivemq/graphs/contributors).

## Security Contact Information

See [SECURITY.md](./SECURITY.md) to raise any security related issues.

## Support
Contact the project developers via the project's "dev" list.

* https://accounts.eclipse.org/mailing-list/ecsp-dev

## Troubleshooting

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details about raising an issue and submitting a pull request to us.

## License

This project is licensed under the Apache 2.0 License. See the [LICENSE](./LICENSE) file for details.

## Announcements

All updates to this component are documented in [releases page](https://github.com/eclipse-ecsp/hivemq/releases).
For the available versions, see the [tags on this repository](https://github.com/eclipse-ecsp/hivemq/tags).

