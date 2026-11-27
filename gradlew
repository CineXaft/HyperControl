#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
exec /usr/lib/jvm/java-21-openjdk-arm64/bin/java -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
