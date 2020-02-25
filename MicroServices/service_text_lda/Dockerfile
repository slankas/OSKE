# Name: openke_ms_topic
# Purpose: extacts topics using LDA from a document corpus
#
# Note: needed to run as defined users and group - received error otherwise
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command: (from current directory)
#     docker build -t="openke_ms_topic" .
#
FROM openke_base as base
COPY . /root/install/MicroServices

RUN cd /root/install/MicroServices/ && \
    mvn clean compile dependency:copy-dependencies package

#FROM openjdk:8u181-jdk
RUN mkdir /opt/topicModeler && \
    mkdir /opt/topicModeler/classes && \
    mkdir /opt/topicModeler/lib
RUN cp -r /root/install/MicroServices/target/classes /opt/topicModeler/ && \
    cp /root/install/MicroServices/target/dependency/* /opt/topicModeler/lib/

RUN groupadd -g 549 -r tmgroup
RUN useradd -d /opt/topicModeler -u 549 -g tmgroup tmuser

RUN chown -R tmuser:tmgroup /opt/topicModeler

WORKDIR /opt/topicModeler
USER tmuser
ENTRYPOINT ["java", "-Xmx4g", "-classpath", "classes:lib/*", "edu.ncsu.las.topicmodel.api.Main"]
EXPOSE 9001
