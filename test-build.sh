#!/bin/bash -x
#set -euo pipefail

TZ="Asia/Tokyo"
DEVELOP_REPO_URL="http://ec2-52-199-35-248.ap-northeast-1.compute.amazonaws.com"
DEVELOP_REPO_NAME="repo"

export TZ
export DEVELOP_REPO_URL
export DEVELOP_REPO_NAME

  echo "@@@@@@@@@@@ env @@@@@@@@@@@@"
env
  echo "@@@@@@@@@@@ env @@@@@@@@@@@@"
#source $HOME/build-script/travis-ci/nablarch_env

cd ${TRAVIS_BUILD_DIR}

### Main Build.
# if it creates pull request, execute `gradlew build` only.
# if it merges pull request to develop branch or dilectly commit on develop branch, execute `gradlew uploadArchives`.
# Waning, TRAVIS_PULL_REQUEST variable is 'false' or pull request number, 1,2,3 and so on.
if [ "${TRAVIS_PULL_REQUEST}" == "false" -a "${TRAVIS_BRANCH}" == "develop"  ]; then
##if false; then
  ./gradlew clean test uploadArchives -PnablarchRepoUsername=${NABLARCH_USER} -PnablarchRepoPassword=${NABLARCH_PASS} \
                           -PnablarchRepoReferenceUrl=${DEVELOP_REPO_URL} -PnablarchRepoReferenceName=${DEVELOP_REPO_NAME} \
                           -PnablarchRepoDeployUrl=dav:${DEVELOP_REPO_URL} -PnablarchRepoName=${DEVELOP_REPO_NAME} \
                           -PdevelopLibUrl=${DEVELOP_REPO_URL}/${DEVELOP_REPO_NAME} --no-daemon
else
  echo "@@@@@@@@@@@ test @@@@@@@@@@@@"
  hostname
  cat $0


  ls -la /tmp
  ./gradlew clean test -Djava.net.preferIPv4Stack=true -PnablarchRepoUsername=hoge -PnablarchRepoPassword=hoge \
                  -PnablarchRepoReferenceUrl=${DEVELOP_REPO_URL} -PnablarchRepoReferenceName=${DEVELOP_REPO_NAME} \
                  -PnablarchRepoDeployUrl=dav:${DEVELOP_REPO_URL} -PnablarchRepoName=${DEVELOP_REPO_NAME} \
                  -PdevelopLibUrl=${DEVELOP_REPO_URL}/${DEVELOP_REPO_NAME} --no-daemon
fi
