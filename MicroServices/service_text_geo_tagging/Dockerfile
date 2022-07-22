# Name: las/openke_ms_geotag
# Purpose: detects geographical locations within unstructured text using the CLAVIN library
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command: (from this directory)
#     docker build -t="openke_ms_geotag" .
#
FROM rockylinux:9.0.20220720 as base
RUN yum update -y
RUN yum -y install java-17-openjdk maven-openjdk17 openssh-clients wget git unzip curl

# Don't like the following, but java 11 is the default.  This is highly version dependent
RUN alternatives --set java /usr/lib/jvm/java-17-openjdk-17.0.3.0.7-1.el9_0.x86_64/bin/java


COPY . /root/install/MicroServices
RUN cd  /root/install/MicroServices && \
    mvn clean compile dependency:copy-dependencies package

# Perform the inital source copy
RUN mkdir /opt/clavin && \
    mkdir /opt/clavin/classes && \
    mkdir /opt/clavin/lib && \
    cp -r /root/install/MicroServices/target/classes /opt/clavin/ && \
    cp -r /root/install/MicroServices/target/dependency/* /opt/clavin/lib/

# Expand the geonames gazeeter
RUN mkdir --parents /opt/clavin/index && \
    curl -o /opt/clavin/index/allCountries.zip http://download.geonames.org/export/dump/allCountries.zip
RUN cd /opt/clavin/index && unzip allCountries.zip
RUN mkdir --parents /opt/clavin/index/src/main/resources/
RUN cp /root/install/MicroServices/resources/SupplementaryGazetteer.txt /opt/clavin/index/src/main/resources/
RUN cd /opt/clavin/index && java -Xmx4g -classpath "/opt/clavin/lib/*" com.bericotech.clavin.index.IndexDirectoryBuilder

FROM eclipse-temurin:17
RUN mkdir /opt/clavin && chown 1000:1000 /opt/clavin
USER 1000
COPY --from=base /opt/clavin /opt/clavin


WORKDIR /opt/clavin
ENTRYPOINT ["java", "-Xmx4g", "-classpath", "classes:lib/*", "edu.ncsu.las.clavin.api.ClavinNerdMain"]
EXPOSE 9001
