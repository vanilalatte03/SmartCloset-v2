#!/bin/sh

APP_HOME=$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P) || exit

if [ -z "$GRADLE_USER_HOME" ]; then
    GRADLE_USER_HOME="$APP_HOME/.gradle-user-home"
    export GRADLE_USER_HOME
fi

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVACMD" ]; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
        exit 1
    fi
else
    JAVACMD=$(command -v java) || {
        echo "ERROR: java executable was not found in PATH." >&2
        exit 1
    }
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" \
    ${DEFAULT_JVM_OPTS:-} \
    ${JAVA_OPTS:-} \
    ${GRADLE_OPTS:-} \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
