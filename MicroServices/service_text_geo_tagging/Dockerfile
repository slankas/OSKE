# Name: las/collector
# Purpose: detects geographical locations within unstructured text using the CLAVIN library
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command: (from this directory)
#     docker build -t="openke_ms_geotag" .
#
# Execution
#     export SERVICE_URL=http://0.0.0.0:9001/geoTagger/
#     export SERVICE_INDEX_DIRECTORY=/opt/clavin/index/IndexDirectory
#     docker run -p 9004:9001 -e SERVICE_URL -e SERVICE_INDEX_DIRECTORY openke_ms_geotag
#
# Test commands:
# curl http://127.0.0.1:9004/geoTagger/v1/process -d '{ "text" : "John lives in Raleigh, North Carolina" }' -H "Content-Type: application/json"
# curl http://127.0.0.1:9004/geoTagger/v1/statistics
#
FROM rockylinux:9
RUN dnf update -y
RUN dnf -y install java-17-openjdk java-17-openjdk-devel maven  unzip
ENV JAVA_HOME /usr/lib/jvm/java-17

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

RUN chown -R 1000:1000 /opt/clavin
USER 1000


WORKDIR /opt/clavin
ENTRYPOINT ["/usr/lib/jvm/java-17/bin/java", "-Xmx4g", "-classpath", "classes:lib/*", "edu.ncsu.las.clavin.api.ClavinNerdMain"]
EXPOSE 9001
