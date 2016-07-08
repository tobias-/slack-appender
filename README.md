Slack Appender ("Logger") for log4j [![Build Status](https://travis-ci.org/tobias-/slack-appender.svg?branch=master)](https://travis-ci.org/tobias-/slack-appender)
=========================

Log4j Appender for sending slack messages


Dependencies
============

For now, depends on both gson and okhttp.


Using the API
=============================

*TODO*

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

Example with both Console and Slack

```
log4j.appender.Foo=org.apache.log4j.ConsoleAppender
log4j.appender.Foo.layout=org.apache.log4j.PatternLayout
log4j.appender.Foo.layout.conversionPattern=%-5p - [%t] %-26.26c{1} - %m\n

log4j.appender.Bar=be.olsson.slackappender.SlackAppender
log4j.appender.Bar.layout=org.apache.log4j.PatternLayout
log4j.appender.Bar.layout.conversionPattern=%-5p - [%t] %-26.26c{1} - %n

log4j.rootLogger=INFO,Foo,Bar
```

Minimal:
```
log4j.appender.Slack=be.olsson.slackappender.SlackAppender
log4j.rootLogger=INFO,Slack

```


Build
-----

    ./gradlew build

Please note that the tests will *NOT* work unless `SLACK_WEBHOOK` is set as an environment variables (or java properties/ ~/.gradle/gradle.properties).

Releasing
---------

    SLACK_WEBHOOK=https://hooks.slack.com/services/xxxxxx/yyyyyyy/zzzzzzz ./gradlew release

