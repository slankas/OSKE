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
# docker run -p 9005:9001 openke_ms_topic
# curl http://127.0.0.1:9005/topicmodel/v1/statistics

FROM rockylinux:9
RUN dnf update -y
RUN dnf -y install java-17-openjdk java-17-openjdk-devel maven
ENV JAVA_HOME /usr/lib/jvm/java-17

COPY . /root/install/MicroServices

RUN cd /root/install/MicroServices/ && \
    mvn clean compile dependency:copy-dependencies package

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
ENTRYPOINT ["/usr/lib/jvm/java-17/bin/java", "-Xmx4g", "-classpath", "classes:lib/*", "edu.ncsu.las.topicmodel.api.Main"]
EXPOSE 9001
