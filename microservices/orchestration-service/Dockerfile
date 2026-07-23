# Start with a base image containing Java runtime
FROM eclipse-temurin:21-jdk-alpine

# Add Maintainer Info
LABEL maintainer="amollimaye"

# Add a volume pointing to /tmp
VOLUME /tmp

EXPOSE 8096

RUN addgroup -S spring && adduser -S spring -G spring

ARG JAR_FILE=target/*.jar

ADD ${JAR_FILE} orchestration-service.jar
RUN chown spring:spring /orchestration-service.jar

USER spring

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/orchestration-service.jar"]
