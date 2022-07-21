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
FROM rockylinux:9.0.20220720 as base
RUN yum update -y
RUN yum -y install java-17-openjdk maven-openjdk17 
# Don't like the following, but java 11 is the default.  This is highly version dependent
RUN alternatives --set java /usr/lib/jvm/java-17-openjdk-17.0.3.0.7-1.el9_0.x86_64/bin/java

COPY . /root/install/MicroServices
RUN cd /root/install/MicroServices && \
    mvn clean compile dependency:copy-dependencies package

FROM eclipse-temurin:17
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
