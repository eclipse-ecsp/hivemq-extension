FROM eclipseecsp/ecsp-base-java17:1.0.0

RUN mkdir /extensions /logback-ext
RUN apk update && apk add unzip

ARG API_WAR_NAME
ARG PROJECT_JAR_VERSION
ENV API_WAR_NAME ${API_WAR_NAME}
ENV PROJECT_JAR_VERSION ${PROJECT_JAR_VERSION}

COPY target/${API_WAR_NAME}-${PROJECT_JAR_VERSION}-distribution.zip /extensions/
ADD logback-ext.zip /logback-ext/

RUN unzip /extensions/*.zip -d /extensions/
RUN unzip /logback-ext/logback-ext.zip -d /
RUN rm /extensions/*.zip
RUN rm /logback-ext/logback-ext.zip


USER nobody:nobody

ENTRYPOINT ["/bin/sh", "-c", "cp -r /extensions/* /target/."]