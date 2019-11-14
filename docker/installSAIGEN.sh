#!/usr/bin/env bash
cd /test

git clone https://<TOKEN>@github.com/natanieljr/droidmate-saigen.git

cd droidmate-saigen

./gradlew clean build

cd /test
