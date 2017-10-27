#!/bin/bash

scd ret -euo pipefail

SYNDESIS_VERSION=${1:-1.1.0}
ATLASMAP_VERSION=${2:-1.31.0}
FROM_REGISTRY=${3:-docker-registry.engineering.redhat.com/jboss-fuse-7-tech-preview}
TO_REGISTRY=${4:-registry.fuse-ignite.openshift.com/fuse-ignite}
IMAGES=${5-" fuse-ignite-mapper:${ATLASMAP_VERSION} fuse-ignite-rest:${SYNDESIS_VERSION} fuse-ignite-ui:${SYNDESIS_VERSION} fuse-ignite-verifier:${SYNDESIS_VERSION}"}

# Push all images to target regitry
# Also push links to head versions (i.e. 1.0 -> 1.0.0)
for image in ${IMAGES}; do
  head_image=$(echo $image | sed -e 's/^\(.*\)\(\.[^.]*\)$/\1/')
  docker pull ${FROM_REGISTRY}/${image}
  docker tag ${FROM_REGISTRY}/${image} ${TO_REGISTRY}/${image}
  docker tag ${TO_REGISTRY}/${image} ${TO_REGISTRY}/${head_image}
  docker push ${TO_REGISTRY}/${image}
  docker push ${TO_REGISTRY}/${head_image}
done

# Push OpenShift OAuth proxy to ignite registry:
OAUTH_PROXY="oauth-proxy:v1.0.0";
docker pull "docker.io/openshift/${OAUTH_PROXY}"
docker tag "docker.io/openshift/${OAUTH_PROXY}" "${TO_REGISTRY}/${OAUTH_PROXY}"
docker push "${TO_REGISTRY}/${OAUTH_PROXY}"

# Push FIS s2i builder image (decoupled from Syndesis version)
S2I_IMAGE_SRC="registry.access.redhat.com/jboss-fuse-6/fis-java-openshift:2.0-9"
S2I_IMAGE_TARGET="fuse-ignite-java-openshift:1.0"
docker pull ${S2I_IMAGE_SRC}
docker tag ${S2I_IMAGE_SRC} ${TO_REGISTRY}/${S2I_IMAGE_TARGET}
head_s2i_image=$(echo $S2I_IMAGE_TARGET | sed -e 's/^\(.*\)\(\.[^.]*\)$/\1/')
docker tag ${TO_REGISTRY}/$S2I_IMAGE_TARGET ${TO_REGISTRY}/${head_s2i_image}
docker push ${TO_REGISTRY}/$S2I_IMAGE_TARGET
docker push ${TO_REGISTRY}/${head_s2i_image}
