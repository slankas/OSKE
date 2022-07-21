# Name: openke_ms_temporal
# Purpose: Extracts time references from unstructured text using the HeidelTime library: https://github.com/HeidelTime/heideltime
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command: (from current directory)
#     docker build -t="openke_ms_temporal" .
#
FROM openke_base as base
COPY . /root/install/MicroServices

RUN cd /root/install/MicroServices/ && \
    mvn clean compile dependency:copy-dependencies package

FROM eclipse-temurin:17
RUN mkdir /opt/heideltimeAPI && \
    mkdir /opt/heideltimeAPI/classes && \
    mkdir /opt/heideltimeAPI/lib
COPY --from=base /root/install/MicroServices/target/classes /opt/heideltimeAPI/classes/
COPY --from=base /root/install/MicroServices/target/dependency/* /opt/heideltimeAPI/lib/
COPY --from=base /root/install/MicroServices/docker_config.props /opt/heideltimeAPI/docker_config.props
COPY --from=base /root/install/MicroServices/english-bidirectional /opt/heideltimeAPI/english-bidirectional

RUN chown -R 1000:1000 /opt/heideltimeAPI

USER 1000
WORKDIR /opt/heideltimeAPI
ENTRYPOINT ["java", "-Xmx8g", "-XX:+ExitOnOutOfMemoryError", "-classpath", "classes:lib/*", "edu.ncsu.las.api.HeidelTimeMain"]
EXPOSE 9001
