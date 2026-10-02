FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
RUN apk add --no-cache maven
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl infraestructura/discovery-server -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl bash
COPY --from=build /app/infraestructura/discovery-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]