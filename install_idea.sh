#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${DIR} || exit
IDEA_BUILD_VERSION=$(egrep '^[^#]*idea.version.*=' "${DIR}/build/build.properties" | sed 's/^.*=//')
IDEA_APP="https://download.jetbrains.com/idea/ideaIC-${IDEA_BUILD_VERSION}.tar.gz"

if [[ ! -e "idea-IC-${IDEA_BUILD_VERSION}" ]]; then
  echo "Downloading $IDEA_APP"
  curl -fL $IDEA_APP | tar xz \
  && rm -rf ".*${IDEA_APP_VERSION}.tar.gz"
fi
