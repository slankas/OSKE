# Name: openke_ms_temporal
# Purpose: Extracts time references from unstructured text using the HeidelTime library: https://github.com/HeidelTime/heideltime
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command: (from current directory)
#     docker build -t="openke_ms_temporal" .
#

#
# export SERVICE_URL=http://0.0.0.0:9001/temporalTagger/
# export SERVICE_CONFIGURATION_FILE=/opt/heideltimeAPI/docker_config.props
# docker run -p 9003:9001 -e SERVICE_URL -e SERVICE_CONFIGURATION openke_ms_temporal
#
# curl http://127.0.0.1:9003/temporalTagger/v1/statistics
# curl http://127.0.0.1:9003/temporalTagger/v1/extract -d '{ "text":"John went to the park on Monday.", "published_date" : { "date":"2023-05-30T10:00:00.00Z"}}' -H "Content-Type: application/json"

FROM centos:7

RUN yum update -y
RUN yum -y install java maven

COPY . /root/install/MicroServices

RUN cd /root/install/MicroServices/ && \
    mvn clean compile dependency:copy-dependencies package

RUN mkdir /opt/heideltimeAPI && \
    mkdir /opt/heideltimeAPI/classes && \
    mkdir /opt/heideltimeAPI/lib
RUN cp -r /root/install/MicroServices/target/classes /opt/heideltimeAPI
RUN cp -r /root/install/MicroServices/target/dependency/* /opt/heideltimeAPI/lib/
RUN cp /root/install/MicroServices/docker_config.props /opt/heideltimeAPI/
RUN cp -r /root/install/MicroServices/english-bidirectional /opt/heideltimeAPI/

RUN chown -R 1000:1000 /opt/heideltimeAPI

USER 1000
WORKDIR /opt/heideltimeAPI
ENTRYPOINT ["java", "-Xmx8g", "-XX:+ExitOnOutOfMemoryError", "-classpath", "classes:lib/*", "edu.ncsu.las.api.HeidelTimeMain"]
EXPOSE 9001
