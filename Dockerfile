# syntax=docker/dockerfile:1
FROM openjdk:23-jdk
LABEL authors="Lukynux"
COPY ${JAR_FILE} app.jar
ARG JAR_FILE=target/*.jar
ENTRYPOINT ["java","-jar","/app.jar"]