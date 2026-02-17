# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY java-backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY java-backend/src ./src
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV PORT=5000
EXPOSE ${PORT}
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
