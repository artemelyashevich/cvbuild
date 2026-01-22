FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY cvbuild /app
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/cvbuild-0.0.1-SNAPSHOT.jar cvbuild.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "cvbuild.jar"]