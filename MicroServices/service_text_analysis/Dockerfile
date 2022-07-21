# Name: cap_ms_textrank
# Purpose: Textrank service to producde keywords, keyphrases, summaries, and back of the book index
# requires: redis server available listening with service_name of openke_redis
# Notes: Attempted to use python3.9.0-strech as the the base image, but could not compile cld2-cffi
#
# Version: 1.0
# History
# 20170405 Initial creation
# 20210406 Added LDA, upgrade to latest ubuntu 18.04, and python 3.7.7

# Build Command:
#     docker build -t="cap_ms_textrank" .
#
FROM ubuntu:20.04

RUN apt-get update
RUN apt-get install -y build-essential
RUN DEBIAN_FRONTEND="noninteractive" apt-get -y install tzdata
RUN apt-get install -y autoconf automake gdb git libffi-dev zlib1g-dev libssl-dev liblzma-dev zlib1g-dev libbz2-dev libsqlite3-dev libffi-dev tcl-dev wget curl

# Install python 3.10.5 from source
RUN mkdir /install
RUN wget -q --directory-prefix=/install https://www.python.org/ftp/python/3.10.5/Python-3.10.5.tgz
RUN cd /install && tar xf Python-3.10.5.tgz && cd Python-3.10.5 && ./configure && make install
RUN rm -rf /install

COPY . /root/install/cap

RUN pip3.10 install --upgrade pip wheel
RUN pip3.10 install -r /root/install/cap/requirements.txt
RUN CFLAGS="-Wno-narrowing"  pip3.10 install cld2-cffi

RUN python3.10 -c "import nltk;nltk.download('book', download_dir='/usr/share/nltk_data', halt_on_error=False)"
RUN python3.10 -c "import nltk;nltk.download('omw-1.4', download_dir='/usr/share/nltk_data', halt_on_error=False)"

RUN mkdir /opt/textAnalysis && \
    mkdir /opt/textAnalysis/gunicorn && \
    cp /root/install/cap/*.py /opt/textAnalysis/ && \
	cp -r /root/install/cap/summarization /opt/textAnalysis/ && \
    cp /root/install/cap/gunicorn/* /opt/textAnalysis/gunicorn && \
    rm -rf /root/install /root/.ssh

WORKDIR /opt/textAnalysis

USER 1000

ENTRYPOINT ["/usr/local/bin/gunicorn", "--config", "/opt/textAnalysis/gunicorn/gunicorn.conf.py", "--log-config", "/opt/textAnalysis/gunicorn/logging.conf", "-b", ":8000", "app:app"]
EXPOSE 8000
