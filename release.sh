#!/usr/bin/env bash

set -eo pipefail

# Trap not-normal exit signals: 1/HUP, 2/INT, 3/QUIT, 15/TERM
trap catch_sig 1 2 3 15
# Trap errors (simple commands exiting with a non-zero status)
trap 'catch_err ${LINENO}' ERR

function clean_exit {
  echo "Exiting - version state may be inconsistent!"
  exit "$1"
}

function catch_err() {
  local PARENT_LINENO="$1"
  local MESSAGE="$2"
  local CODE="${3:-1}"
  if [[ -n "$MESSAGE" ]] ; then
    echo "Error on or near line ${PARENT_LINENO}: ${MESSAGE}; exiting with status ${CODE}"
  else
    echo "Error on or near line ${PARENT_LINENO}; exiting with status ${CODE}"
  fi
  clean_exit "${CODE}"
}

function catch_sig() {
    local exit_status=$?
    clean_exit $exit_status
}

deploy_site() {
    local upstream_url="${1}"
    local commit="${2}"
    local root_dir="$PWD"

    mkdir -p "$PWD/target"
    cd "$PWD/target"
    git clone --depth=1 --branch "gh_site" "${upstream_url}" "gh_site" || true

    if [ ! -d "gh_site" ]; then
        echo "[INFO] Initial creation of new orphan branch gh_site."
        git clone --depth=1 "${upstream_url}" "gh_site"
        cd gh_site || exit 1
        git checkout --orphan "gh_site"
        rm '.gitignore'
    fi

    rm -rf "${PWD}/."
    rsync -a "$root_dir/target/staging/." .
    git add .
    git commit -m "[SITE] update site commit [$commit]"
    git push origin gh_site
    cd "${root_dir}"
}

if [ $# -ne 2 ]; then
  echo "Usage: release.sh <release_version> <new_version>"
  exit 1
fi

release_version=$1
new_version=$2

mvn versions:set "-DnewVersion=$release_version" -DgenerateBackupPoms=false

git commit -a -m "$release_version release"
git tag -a "$release_version" -a -m "$release_version release"

mvn clean site site:stage deploy -DskipTests -Prelease,docs
deploy_site "$(git remote get-url origin)" "$(git rev-parse HEAD)"

mvn versions:set "-DnewVersion=$new_version" -DgenerateBackupPoms=false
git commit -a -m "Preparing $new_version iteration"

git push
git push origin "$release_version"
