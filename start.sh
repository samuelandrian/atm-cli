#!/bin/bash
set -e

# Compile and package the application
mvn clean package -DskipTests

# Run the executable JAR
java -jar target/atm-cli.jar
