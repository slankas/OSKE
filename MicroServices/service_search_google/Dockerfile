# Name: las/openke_ms_google
# Purpose: Provides a rest-based service to search Google
#
# Version: 1.0
# History
# 20230524 Re-created service using the the googlesearch-python API: https://github.com/Nv7-GitHub/googlesearch
#
# Build Command:
#     docker build -t="openke_ms_google" .
#
# RUN:
#     docker run -p EXPOSED_PORT_NUM:8000 openke_ms_google
#
FROM python:3.11
RUN pip3.11 install --upgrade pip setuptools wheel

COPY . /root/install/MicroServices
RUN mkdir /opt/googleapi && \
    mkdir /opt/googleapi/gunicorn && \
    cp /root/install/MicroServices/*.py /opt/googleapi/ && \
    cp /root/install/MicroServices/requirements.txt /opt/googleapi && \
    cp /root/install/MicroServices/gunicorn/*.conf.py /opt/googleapi/gunicorn

WORKDIR /opt/googleapi


RUN pip3.11 install -r requirements.txt


USER 1000
ENTRYPOINT ["/usr/local/bin/gunicorn", "--config", "/opt/googleapi/gunicorn/gunicorn.conf.py", "--log-config", "/opt/googleapi/gunicorn/logging.conf.py", "-b", ":8000", "app:app"]
EXPOSE 8000
