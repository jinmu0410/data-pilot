FROM maven:latest AS builder
WORKDIR /build
COPY . .
RUN mvn clean install -Dmaven.test.skip=true -U -e -X -B -N
RUN mvn clean install -Dmaven.test.skip=true -U -e -X -B -f data-pilot-common/pom.xml
RUN mvn clean package -Dmaven.test.skip=true -U -e -X -B -f data-pilot-flow/pom.xml

FROM eclipse-temurin:21
COPY --from=builder /build/data-pilot-flow/target/*.jar app.jar
VOLUME /tmp
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
