# Name: las/collector
# Purpose: extracts camera meta-data information from images
#
# Version: 1.0
# History
# 20170405 Initial creation
# 20230530 Update to Java 17
#
# Build Command:
#     docker build -t="openke_ms_exif" .
#
# Run:
#     docker run -p 9001:9001 openke_ms_exif
#
# Send a test file with curl:
#     curl http://127.0.0.1:9001/exif/v1/extract -F 'file=@test.jpg'
#

FROM rockylinux:9
RUN dnf update -y
RUN dnf -y install java-17-openjdk java-17-openjdk-devel maven
ENV JAVA_HOME /usr/lib/jvm/java-17

COPY . /root/install/MicroServices
RUN cd /root/install/MicroServices && \
    mvn clean compile dependency:copy-dependencies package

RUN mkdir /opt/exifExtractor  && \
    mkdir /opt/exifExtractor/classes && \
    mkdir /opt/exifExtractor/lib
RUN cp -r /root/install/MicroServices/target/classes /opt/exifExtractor/
RUN cp -r /root/install/MicroServices/target/dependency/* /opt/exifExtractor/lib/

RUN chown -R 1000:1000 /opt/exifExtractor

USER 1000
WORKDIR /opt/exifExtractor
ENTRYPOINT ["/usr/lib/jvm/java-17/bin/java", "-Xmx1g", "-classpath", "classes:lib/*", "edu.ncsu.las.image.api.Main"]
EXPOSE 9001
