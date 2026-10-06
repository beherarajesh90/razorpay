# Build one service from the reactor. Usage:
#   docker build --build-arg SERVICE=merchant-service -t razorpay/merchant-service .
ARG SERVICE

FROM maven:3.9-eclipse-temurin-25 AS build
ARG SERVICE
WORKDIR /workspace
COPY . .
RUN mvn -B -q -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:25-jre
ARG SERVICE
WORKDIR /app
COPY --from=build /workspace/${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
