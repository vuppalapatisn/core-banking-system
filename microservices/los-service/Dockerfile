# Start with a base image containing Java runtime
FROM eclipse-temurin:21-jdk-alpine

# Add Maintainer Info
LABEL maintainer="amollimaye"

# Add a volume pointing to /tmp
VOLUME /tmp

EXPOSE 8098

RUN addgroup -S spring && adduser -S spring -G spring

ARG JAR_FILE=target/*.jar

ADD ${JAR_FILE} los-service.jar
RUN chown spring:spring /los-service.jar

USER spring

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/los-service.jar"]
