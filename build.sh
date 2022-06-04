#!/bin/bash
./gradlew bootjar
cp build/libs/*.jar build/libs/dbManager.jar
docker build -t dbmanager .
