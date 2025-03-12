#!/bin/bash
echo "Building J2ME Application..."

# Create bin directory if not exists
mkdir -p bin

# Compile Java files
javac -target 1.3 -bootclasspath /opt/Java_ME_platform_SDK_3.0.5/lib/midpapi.zip -d bin src/*.java

# Create JAR file
jar cvf KMRadio.jar -C bin .

echo "Build Complete! JAR file: KMRadio.jar"
