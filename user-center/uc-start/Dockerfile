FROM eclipse-temurin:17-jre

WORKDIR /app
COPY target/uc-start-0.0.1-SNAPSHOT.jar /app/app.jar

# 默认端口，可通过 SERVER_PORT 覆盖
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar"]
