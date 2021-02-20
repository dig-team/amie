FROM openjdk:8-jdk-alpine3.9
RUN apk update and apk add
RUN apk add --update maven

RUN apk add --update git
RUN git clone https://github.com/lajus/amie-utils
RUN cd amie-utils/javatools/ && mvn install
COPY ./. /
CMD ["mvn", "clean", "install"]
