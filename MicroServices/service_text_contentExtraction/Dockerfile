# Version: 1.0
# History
# 20170809 Initial creation
#
#    docker build -t="openke_ms_service_structural" .
#
# Execution: (note a more secure way than directly on the command prompt should be used to set the SERVICE_DATABASE value
# export SERVICE_URL=http://0.0.0.0:9001/structuralExtraction/
# export SERVICE_DATABASE='{"datasourcename":"CollectorDataSource","driver":"org.postgresql.Driver","url":"jdbc:postgresql://serverName:5432/databaseName","user":"userName","password":"password","maxconnections":10}'
# export SERVICE_USER_AGENT='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36'
# docker run -d -p HOSTPORT:9001 -e SERVICE_URL -e SERVICE_DATABASE -e SERVICE_USER_AGENT las/service_structural
#

FROM openke_base as base

RUN groupadd -g 565 -r segroup
RUN useradd -d /opt/structuralExtraction -u 565 -g segroup seuser

COPY . /root/install/MicroServices
RUN cd  /root/install/MicroServices && \
    mvn clean compile dependency:copy-dependencies package
RUN cp -r /root/install/MicroServices/target/classes /opt/structuralExtraction/classes && \
    cp -r /root/install/MicroServices/target/dependency /opt/structuralExtraction/lib && \
    rm -rf /root/install /root/.m2

WORKDIR /opt/structuralExtraction
USER seuser
ENTRYPOINT ["/bin/java", "-Xmx1g", "-classpath", "classes:lib/*", "edu.ncsu.las.api.StructuralExtractorMain"]
EXPOSE 9001
