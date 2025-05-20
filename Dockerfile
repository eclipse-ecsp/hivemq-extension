FROM tomcat:10.1.40-jdk17

RUN mkdir /extensions /logback-ext
RUN apk update && apk add unzip

ARG ARTIFACT_ID
ARG ARTIFACT_VERSION
ENV ARTIFACT_ID ${ARTIFACT_ID}
ENV ARTIFACT_VERSION ${ARTIFACT_VERSION}

COPY target/${ARTIFACT_ID}-${ARTIFACT_VERSION}-distribution.zip /extensions/
ADD logback-ext.zip /logback-ext/

RUN unzip /extensions/*.zip -d /extensions/
RUN unzip /logback-ext/logback-ext.zip -d /
RUN rm /extensions/*.zip
RUN rm /logback-ext/logback-ext.zip


USER nobody:nobody

ENTRYPOINT ["/bin/sh", "-c", "cp -r /extensions/* /target/."]