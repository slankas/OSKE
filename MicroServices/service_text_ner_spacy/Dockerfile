# Name: las/collector
# Purpose: Extracts named entities from unstructured text using the spacy python library
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command:
#     docker build -t="openke_ms_spacy" .
#
FROM python:3.10.5

RUN pip3.10 install --upgrade pip setuptools wheel
RUN pip3.10 install --upgrade spacy flask gunicorn json-logging-py
RUN python3.10 -m spacy download en_core_web_lg

COPY . /root/install/MicroServices

RUN mkdir /opt/spacyNER && \
    mkdir /opt/spacyNER/gunicorn && \
    cp /root/install/MicroServices/*.py /opt/spacyNER/ && \
    cp /root/install/MicroServices/gunicorn/*.conf /opt/spacyNER/gunicorn


USER 1000
WORKDIR /opt/spacyNER
ENTRYPOINT ["/usr/local/bin/gunicorn", "--config", "/opt/spacyNER/gunicorn/gunicorn.conf", "--log-config", "/opt/spacyNER/gunicorn/logging.conf", "-b", ":8000", "app:app"]
EXPOSE 8000
