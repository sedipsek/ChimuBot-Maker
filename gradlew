#!/usr/bin/env sh
set -e
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
CLASSPATH="$CLASSPATH:$APP_HOME/gradle/wrapper/gradle-wrapper-shared-8.5.jar"
CLASSPATH="$CLASSPATH:$APP_HOME/gradle/wrapper/gradle-cli-8.5.jar"
CLASSPATH="$CLASSPATH:$APP_HOME/gradle/wrapper/gradle-base-annotations-8.5.jar"
CLASSPATH="$CLASSPATH:$APP_HOME/gradle/wrapper/gradle-files-8.5.jar"
CLASSPATH="$CLASSPATH:$APP_HOME/gradle/wrapper/gradle-functional-8.5.jar"

if [ -n "$JAVA_HOME" ]; then
  JAVA_EXE="$JAVA_HOME/bin/java"
  if [ ! -x "$JAVA_EXE" ]; then
    echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
    exit 1
  fi
else
  JAVA_EXE="java"
fi

exec "$JAVA_EXE" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
