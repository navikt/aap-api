# Docker multistage layer with a fatty jar stripped of unused rocskdb-instances
FROM alpine:3.20.3 as app
RUN apk --update --no-cache add zip
COPY /app/build/libs/app-all.jar app.jar
RUN zip -d app.jar librocksdbjni-linux32.so librocksdbjni-linux32-musl.so librocksdbjni-linux64.so \
    librocksdbjni-linux-aarch64.so librocksdbjni-linux-aarch64-musl.so librocksdbjni-linux-ppc64le.so \
    librocksdbjni-linux-ppc64le-musl.so librocksdbjni-linux-s390x.so librocksdbjni-linux-s390x-musl.so \
    librocksdbjni-osx-arm64.jnilib librocksdbjni-osx-x86_64.jnilib librocksdbjni-win64.dll


FROM eclipse-temurin:21-jre-alpine
ENV LANG="nb_NO.UTF-8"
ENV LC_ALL="nb_NO.UTF-8"
ENV TZ="Europe/Oslo"
RUN apk --update --no-cache add libstdc++
COPY --from=app app.jar .
CMD ["java", "-XX:ActiveProcessorCount=2", "-jar", "app.jar"]

# use -XX:+UseParallelGC when 2 CPUs and 4G RAM.
# use G1GC when using more than 4G RAM and/or more than 2 CPUs
# use -XX:ActiveProcessorCount=2 if less than 1G RAM.