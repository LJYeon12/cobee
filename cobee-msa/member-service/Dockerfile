FROM eclipse-temurin:21-jdk AS build
ARG MODULE_NAME
WORKDIR /app

COPY gradlew settings.gradle ./
COPY gradle ./gradle

COPY ${MODULE_NAME}/build.gradle ./build.gradle

COPY ${MODULE_NAME}/src ./src
COPY common/build.gradle ./common/
COPY common/src ./common/src

RUN chmod +x ./gradlew
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew -q clean bootJar --refresh-dependencies

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"

EXPOSE 8081

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]