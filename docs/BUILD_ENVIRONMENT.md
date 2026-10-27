# Build environment used for this slice

The preinstalled Java 21 runtime lacked javac, so a full Temurin JDK 21 was downloaded outside the repository under /workspace/scratch/jdk21. The official Gradle 9.8.2 distribution was downloaded outside the repository under /workspace/scratch/gradle-9.8.2.

Java HTTP clients required the environment proxy and system CA trust store. Equivalent verified invocation in this container:

```sh
JAVA_HOME=/workspace/scratch/jdk21 /workspace/scratch/gradle-9.8.2/bin/gradle \
  -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
  -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 \
  -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 \
  check :apps:desktop:packageUberJarForCurrentOS --console=plain
```

Ordinary development environments with a full JDK and direct network access should use the committed wrapper. No proxy settings, private CA files or downloaded JDKs are committed.

Graphical Linux launch was verified on extracted Xvfb under display :99 using the packaged JAR and the downloaded JDK. The virtual display has no GL context; Skiko falls back to software. This is a launch/render check, not a physical audio-device test. No Android SDK or Windows/macOS host is provisioned. Native packageDeb failed because fakeroot is missing.
