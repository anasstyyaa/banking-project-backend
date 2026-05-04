
# This downloads the JDK and Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy your pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the project and skip tests to save time on Render
RUN mvn clean package -DskipTests


# We use a smaller 'JRE' image to keep the deployment fast
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy only the built .jar file from the first step
COPY --from=build /app/target/*.jar app.jar

# Tell Render which port the app runs on
EXPOSE 8080

# The command to start your bank app
ENTRYPOINT ["java", "-jar", "app.jar"]