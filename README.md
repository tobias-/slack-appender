Slack Appender ("Logger") for log4j [![Build Status](https://travis-ci.org/tobias-/slack-appender.svg?branch=master)](https://travis-ci.org/tobias-/slack-appender)
=========================

Log4j Appender for sending slack messages


Dependencies
============

For now, depends on both gson and okhttp.


Using the API with Log4j 1.x
=============================

With Gradle
-----------

        compile 'be.olsson:slack-appender:0.99.7'

With Maven
----------

        <dependency>
                <groupId>be.olsson</groupId>
                <artifactId>slack-appender</artifactId>
                <version>0.99.7</version>
        </dependency>

Using
-----

Example with both Console and Slack

```properties
log4j.appender.Foo=org.apache.log4j.ConsoleAppender
log4j.appender.Foo.layout=org.apache.log4j.PatternLayout
log4j.appender.Foo.layout.conversionPattern=%-5p - [%t] %-26.26c{1} - %m\n

log4j.appender.Bar=be.olsson.slackappender.SlackAppender
log4j.appender.Bar.layout=org.apache.log4j.PatternLayout
log4j.appender.Bar.layout.conversionPattern=%-5p - [%t] %-26.26c{1} - %n

log4j.rootLogger=INFO,Foo,Bar
```

Minimal:
```properties
log4j.appender.Slack=be.olsson.slackappender.SlackAppender
log4j.rootLogger=INFO,Slack

```

Using the API with Log4j 2.x
=============================

With Gradle
-----------

        compile 'be.olsson:slack-appender:*'

With Maven
----------

        <dependency>
                <groupId>be.olsson</groupId>
                <artifactId>slack-appender</artifactId>
                <version>*TBD*</version>
        </dependency>

Using
-----

Example with both Console and Slack (but only error logging goes on slack)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
	<Console name="StdOut" target="SYSTEM_OUT">
	    <PatternLayout pattern="%-5p - [%t] %-26.26c{1} - %X{tag} - %m\n"/>
	</Console>
	<Slack name="Slack" channel="log4jslackchannel">
	    <PatternLayout pattern="%-5p - [%t]"/>
	</Slack>
    </Appenders>
    <Loggers>
	<Root level="info">
	    <AppenderRef ref="StdOut"/>
	    <AppenderRef ref="Slack" level="error"/>
	</Root>
    </Loggers>
</Configuration>
```

Minimal:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
	<Slack name="Slack" channel="log4jslackchannel">
	    <PatternLayout pattern="%-5p - [%t]"/>
	</Slack>
    </Appenders>
    <Loggers>
	<Root level="info">
	    <AppenderRef ref="Slack" level="info"/>
	</Root>
    </Loggers>
</Configuration>

```


Build
-----

    ./gradlew build

Please note that the tests will *NOT* work unless `SLACK_WEBHOOK` is set as an environment variables (or java property or ~/.gradle/gradle.properties).

Releasing
---------

    SLACK_WEBHOOK=https://hooks.slack.com/services/xxxxxx/yyyyyyy/zzzzzzz ./gradlew release

