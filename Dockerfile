FROM eclipse-temurin:21-jre
WORKDIR /app
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 TZ=Asia/Shanghai

COPY community-server/target/community-server-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080 8091

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
