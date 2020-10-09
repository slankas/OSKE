# Name: las/collector
# Purpose: Extracts named entities from unstructured text using the spacy python library
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command:
#     docker build -t="openke_ms_google" .
#
# RUN:
#     docker run -p EXPOSED_PORT_NUM:8000 openke_ms_google
#
FROM python:3.7.3-stretch

RUN pip3.7 install --upgrade pip setuptools
RUN pip3.7 install --upgrade spacy flask gunicorn json-logging-py jsons
RUN pip3.7 install git+https://github.com/slankas/Google-Search-API

COPY . /root/install/MicroServices

RUN mkdir /opt/googleapi && \
    mkdir /opt/googleapi/gunicorn && \
    cp /root/install/MicroServices/*.py /opt/googleapi/ && \
    cp /root/install/MicroServices/gunicorn/*.conf.py /opt/googleapi/gunicorn


USER 1000
WORKDIR /opt/googleapi
ENTRYPOINT ["/usr/local/bin/gunicorn", "--config", "/opt/googleapi/gunicorn/gunicorn.conf.py", "--log-config", "/opt/googleapi/gunicorn/logging.conf.py", "-b", ":8000", "app:app"]
EXPOSE 8000
