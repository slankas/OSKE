# Name: las/collector
# Purpose: retrieves whois data for a domain name
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command: (from this directory)
#     docker build -t="openke_ms_whois" .
#
FROM openke_base as base
COPY . /root/install/MicroServices

RUN cd /root/install/MicroServices && \
    mvn clean compile dependency:copy-dependencies package

#FROM centos:7.5.1804
FROM openjdk:8u181-jdk
RUN mkdir /opt/whois && \
    mkdir /opt/whois/classes && \
    mkdir /opt/whois/lib
COPY --from=base /root/install/MicroServices/target/classes /opt/whois/classes/
COPY --from=base /root/install/MicroServices/target/dependency/* /opt/whois/lib/

RUN chown -R 1000:1000 /opt/whois

USER 1000
WORKDIR /opt/whois
ENTRYPOINT ["java", "-Xmx8g", "-classpath", "classes:lib/*", "edu.ncsu.las.api.WhoIsMain"]
EXPOSE 9001
