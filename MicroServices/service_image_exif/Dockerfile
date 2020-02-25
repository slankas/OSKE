# Name: las/collector
# Purpose: extracts camera meta-data information from images
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command:
#     docker build -t="openke_ms_exif" .
#
FROM centos:7.5.1804 as base
RUN yum update -y
RUN yum -y install java maven

COPY . /root/install/MicroServices
RUN cd /root/install/MicroServices && \
    mvn clean compile dependency:copy-dependencies package

FROM openjdk:8u181-jdk
RUN mkdir /opt/exifExtractor  && \
    mkdir /opt/exifExtractor/classes && \
    mkdir /opt/exifExtractor/lib
COPY --from=base /root/install/MicroServices/target/classes /opt/exifExtractor/classes/
COPY --from=base /root/install/MicroServices/target/dependency/* /opt/exifExtractor/lib/

RUN chown -R 1000:1000 /opt/exifExtractor

USER 1000
WORKDIR /opt/exifExtractor
ENTRYPOINT ["java", "-Xmx1g", "-classpath", "classes:lib/*", "edu.ncsu.las.image.api.Main"]
EXPOSE 9001
