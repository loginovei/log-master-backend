FROM maven:3.9.9-amazoncorretto-21 AS build
WORKDIR /build

COPY log-client-lib/pom.xml log-client-lib/pom.xml
COPY log-client-lib/src log-client-lib/src
RUN cd log-client-lib && mvn install -DskipTests -q

COPY log-master/pom.xml log-master/pom.xml
RUN cd log-master && mvn dependency:go-offline -q
COPY log-master/src log-master/src
RUN cd log-master && mvn package -DskipTests -q

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /build/log-master/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
