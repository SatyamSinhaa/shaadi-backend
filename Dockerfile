# Use official Java Maven image to build the app
FROM maven:3.9.2-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom and source code
COPY pom.xml .
COPY src ./src

# Build the jar
RUN mvn clean package -DskipTests

# Use a smaller JDK image for running
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
