# Name: openke_ms_nlp
# Purpose: runs the Stanford corenlp against text to extract named entities and relations
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command:
#     docker build -t="openke_ms_nlp" .
#
FROM openke_base as base

COPY . /root/install/MicroServices
RUN cd  /root/install/MicroServices && \
    mvn clean compile dependency:copy-dependencies package

FROM openjdk:8u181-jdk
RUN mkdir /opt/nlpAPI  && \
    mkdir /opt/nlpAPI/classes && \
    mkdir /opt/nlpAPI/lib
COPY --from=base /root/install/MicroServices/target/classes /opt/nlpAPI/classes/
COPY --from=base /root/install/MicroServices/target/dependency/* /opt/nlpAPI/lib/

RUN chown -R 1000:1000 /opt/nlpAPI

USER 1000
WORKDIR /opt/nlpAPI
ENTRYPOINT ["java", "-Xmx8g", "-classpath", "classes:lib/*", "edu.ncsu.las.api.StanfordCoreNLPMain"]
EXPOSE 9001
