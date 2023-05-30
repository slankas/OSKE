# Name: openke_ms_spacy
# Purpose: Extracts named entities from unstructured text using the spacy python library
#
# Version: 1.0
# History
# 20170405 Initial creation
#
# Build Command:
#     docker build -t="openke_ms_spacy" .
#
# RUN:
#     docker run -p EXPOSED_PORT_NUM:8000 openke_ms_spacy
#
FROM python:3.11
RUN pip3.11 install --upgrade pip setuptools wheel

COPY . /root/install/MicroServices

RUN mkdir /opt/spacyNER && \
    mkdir /opt/spacyNER/gunicorn && \
    cp /root/install/MicroServices/*.py /opt/spacyNER/ && \
    cp /root/install/MicroServices/gunicorn/*.conf.py /opt/spacyNER/gunicorn && \
    cp /root/install/MicroServices/requirements.txt opt/spacyNER

WORKDIR /opt/spacyNER

RUN pip3.11 install -r requirements.txt
RUN python3.11 -m spacy download en_core_web_lg

USER 1000

ENTRYPOINT ["/usr/local/bin/gunicorn", "--config", "/opt/spacyNER/gunicorn/gunicorn.conf.py", "--log-config", "/opt/spacyNER/gunicorn/logging.conf.py", "-b", ":8000", "app:app"]
EXPOSE 8000
