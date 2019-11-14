#!/usr/bin/env bash
cd /test

git clone https://github.com/uds-se/droidmate.git

cd droidmate

./gradlew clean build install

cd /test
