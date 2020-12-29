FROM adoptopenjdk:11.0.6_10-jre-hotspot-bionic

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} app.jar

COPY ../spread-src-5.0.1.tar.gz /spread-src-5.0.1.tar.gz




ENTRYPOINT ["java", "-jar", "/app.jar"]
