FROM openjdk:9 AS build-env

ADD . /build-env
WORKDIR /build-env
RUN ./gradlew installDist

FROM openjdk:9

COPY --from=build-env /build-env/build/install/* /
ENTRYPOINT ["ls", "-la", "/"]
