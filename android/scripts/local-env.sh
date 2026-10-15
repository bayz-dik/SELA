# Source from apps/android before using ./gradlew in this sandbox.
export JAVA_HOME=/home/hatch/.cache/sela-toolchains/jdk
export ANDROID_HOME=/home/hatch/.cache/sela-android-sdk
export GRADLE_USER_HOME=/home/hatch/.cache/sela-android-gradle
export SELA_MAVEN_BRIDGE=http://127.0.0.1:8766
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.net.preferIPv4Stack=true"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
