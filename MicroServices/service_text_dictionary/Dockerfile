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
FROM rockylinux:9.0.20220720 as base
RUN yum update -y
RUN yum -y install java-17-openjdk maven-openjdk17 openssh-clients wget git unzip

# Don't like the following, but java 11 is the default.  This is highly version dependent
RUN alternatives --set java /usr/lib/jvm/java-17-openjdk-17.0.3.0.7-1.el9_0.x86_64/bin/java


COPY . /root/install/MicroServices
RUN cd /root/install/MicroServices && \
    mvn clean compile dependency:copy-dependencies package

FROM eclipse-temurin:17
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
