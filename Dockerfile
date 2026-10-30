FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/nevcoin-*.jar /app/nevcoin.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/nevcoin.jar"]
