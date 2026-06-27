FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY server/pom.xml server/pom.xml
COPY client/pom.xml client/pom.xml

RUN ./mvnw dependency:go-offline -pl server -am -q

COPY shared/src shared/src
COPY server/src server/src

RUN ./mvnw package -pl server -am -DskipTests -q

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/server/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
