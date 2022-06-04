FROM openjdk:17
COPY build/libs/dbManager.jar /usr/src/app/
WORKDIR /usr/src/app
CMD java -jar dbManager.jar
