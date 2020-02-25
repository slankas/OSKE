# Name: las/collector
# Purpose: converts addresses to lat/long
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command: (from this directory)
#     docker build -t="openke_ms_geocode" .
#
FROM centos:7.5.1804 as base

RUN yum update -y
RUN yum -y install java maven

COPY . /root/install/MicroServices
RUN cd /root/install/MicroServices && \
    mvn clean compile dependency:copy-dependencies package

FROM openjdk:8u181-jdk
RUN mkdir /opt/geoCoder && \
    mkdir /opt/geoCoder/classes && \
    mkdir /opt/geoCoder/lib
COPY --from=base /root/install/MicroServices/target/classes /opt/geoCoder/classes
COPY --from=base /root/install/MicroServices/target/dependency /opt/geoCoder/lib

RUN chown -R 1000:1000 /opt/geoCoder

USER 1000
WORKDIR /opt/geoCoder
ENTRYPOINT ["java", "-Xmx1g", "-classpath", "classes:lib/*", "edu.ncsu.las.geo.api.Main"]
EXPOSE 9001
