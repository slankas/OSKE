#!/bin/sh

# This file will install the latest version of the community
# edition of docker and version 1.24.1 of docker-compose

yum install -y yum-utils   device-mapper-persistent-data   lvm2 git
yum-config-manager     --add-repo     https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce
curl -L "https://github.com/docker/compose/releases/download/1.24.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
systemctl enable docker
systemctl start docker
echo "vm.max_map_count=262144" >> /etc/sysctl.conf
sysctl -w vm.max_map_count=262144
docker -v
docker-compose --version