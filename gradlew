#!/bin/sh

#
# Standard Gradle wrapper launch script (unmodified from Gradle's own template).
# Requires gradle/wrapper/gradle-wrapper.jar to be present -- see SETUP.md:
# this file could not be downloaded in the environment this project was built
# in (no network access to services.gradle.org). Run `gradle wrapper
# --gradle-version 8.9` once with a local Gradle install, or simply open this
# project in Android Studio, which regenerates the wrapper jar automatically.
#

PRG="$0"
APP_HOME=$(cd "$(dirname "$PRG")" && pwd)
APP_NAME="Gradle"
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
