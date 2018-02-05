# See : https://github.com/docker-library/openjdk/issues/145
FROM openjdk:8

RUN mkdir -p /app
WORKDIR /app

ADD ./gradle/ ./gradle/
ADD ./gradlew ./gradlew
RUN chmod +x ./gradlew && ./gradlew -v

ADD ./ ./
RUN ./gradlew installDist
ENV PATH $PATH:/app/build/install/app/bin
CMD ["app"]
