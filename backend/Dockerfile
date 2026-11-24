# Multi-stage build: compila con Maven y genera una imagen chica solo con el JRE.
#   docker build --build-arg MODULE=recipe-service -t nutriplan/recipe-service backend/
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
COPY . .
RUN mvn -q -B -pl ${MODULE} -am package -DskipTests \
 && cp ${MODULE}/target/*.jar /app.jar \
 && java -Djarmode=tools -jar /app.jar extract --layers --launcher --destination /layers

FROM eclipse-temurin:21-jre
WORKDIR /app
# Capas: dependencias (cambian poco) primero => rebuilds más rápidos
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./
# No correr como root
RUN useradd -r -u 1001 app
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
