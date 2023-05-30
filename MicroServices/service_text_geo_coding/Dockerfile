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
# Running:
#     export APPLICATION_PROPERTIES='{"geo_api":{"service_url":"http://0.0.0.0:9001/geo/","cacheSize":20000,"maxSleepTime":1000},"providers":[{"longitudeField":"lon","requestRate":1,"parentArray":"","name":"openStreetMap","latitudeField":"lat","userAgent":" IDENTENIFYING_EMAIL_ADDRESS","maxPerDay":86400,"restEndPoint":"http://nominatim.openstreetmap.org/search?format=json&q="},{"longitudeField":"lon","requestRate":1,"parentArray":"","name":"locationIQ","latitudeField":"lat","userAgent":" IDENTENIFYING_EMAIL_ADDRESS ","maxPerDay":10000,"restEndPoint":"https://locationiq.org/v1/search.php?key=LOCATION_IQ_KEY&format=json&q="}]}'
#     docker run -p 9002:9001 openke_ms_geocode
#
# Sample execution:
#     curl "http://127.0.0.1:9002/geo/v1/geoCode?location=Raleigh,North%20Carolina"

FROM rockylinux:9
RUN dnf update -y
RUN dnf -y install java-17-openjdk java-17-openjdk-devel maven
ENV JAVA_HOME /usr/lib/jvm/java-17

COPY . /root/install/MicroServices
RUN cd /root/install/MicroServices && \
    mvn clean compile dependency:copy-dependencies package

RUN mkdir /opt/geoCoder && \
    mkdir /opt/geoCoder/classes && \
    mkdir /opt/geoCoder/lib
RUN cp -r /root/install/MicroServices/target/classes /opt/geoCoder
RUN cp -r /root/install/MicroServices/target/dependency/* /opt/geoCoder/lib

RUN chown -R 1000:1000 /opt/geoCoder

USER 1000
WORKDIR /opt/geoCoder
ENTRYPOINT ["/usr/lib/jvm/java-17/bin/java", "-Xmx1g", "-classpath", "classes:lib/*", "edu.ncsu.las.geo.api.Main"]
EXPOSE 9001
