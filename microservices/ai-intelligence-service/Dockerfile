# Start with a base image containing Java runtime
FROM eclipse-temurin:21-jdk-alpine

# Add Maintainer Info
LABEL maintainer="amollimaye"

# Add a volume pointing to /tmp
VOLUME /tmp

EXPOSE 8102

RUN addgroup -S spring && adduser -S spring -G spring

ARG JAR_FILE=target/*.jar

ADD ${JAR_FILE} ai-intelligence-service.jar
RUN chown spring:spring /ai-intelligence-service.jar

USER spring

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/ai-intelligence-service.jar"]
