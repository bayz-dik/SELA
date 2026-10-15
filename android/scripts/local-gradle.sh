#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Test/Javac child JVMs also need IPv4 in this sandbox.
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.net.preferIPv4Stack=true"
export JAVA_HOME=/home/hatch/.cache/sela-toolchains/jdk
export ANDROID_HOME=/home/hatch/.cache/sela-android-sdk
export GRADLE_USER_HOME=/home/hatch/.cache/sela-android-gradle
export SELA_MAVEN_BRIDGE=http://127.0.0.1:8766
export PATH="$JAVA_HOME/bin:$PATH"
exec "$JAVA_HOME/bin/java" -Xmx1536m \
 --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED \
 --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.prefs/java.util.prefs=ALL-UNNAMED \
 --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
 --add-opens=java.base/java.nio.charset=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED \
 --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED \
 -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en -Duser.variant \
 -Dorg.gradle.internal.instrumentation.agent=false \
 -cp /home/hatch/.cache/sela-toolchains/gradle-8.9/lib/gradle-launcher-8.9.jar org.gradle.launcher.GradleMain \
 --no-daemon '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' "$@"
