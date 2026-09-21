FROM docker.elastic.co/elasticsearch/elasticsearch:8.12.2

ARG IK_VERSION=8.12.2

RUN bin/elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/${IK_VERSION}
