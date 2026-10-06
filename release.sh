#!/bin/bash
# Maven+gitflow release script, using calendar versions YY.N.P:
#   YY  two-digit year of the release
#   N   release number within that year, starting at 1
#   P   patch number of that release, starting at 0
#
# Usage:
#   ./release.sh           next release of the year, such as 26.2.0 after 26.1.x
#   ./release.sh --patch   patch of the latest release from main, such as 26.1.1 after 26.1.0

set -e

mode="release"
case "$1" in
  "") ;;
  --patch) mode="patch";;
  *) echo "Usage: $0 [--patch]" >&2; exit 1;;
esac

case "${OSTYPE}" in
  darwin*)
        default="\x1B[0m"
        cyan="\x1B[36m"
        creeol="\r\033[K"
        ;;
  *)
        default="\e[0m"
        cyan="\e[36m"
        creeol="\r\033[K"
        ;;
esac

fn_print_info(){
  echo -en "${creeol}[${cyan} INFO ${default}] $*"
  echo -en "\n"
}

fn_fail(){
  echo -e "[ ERROR ] $*" >&2
  exit 1
}

##########################
# Release metadata
##########################

fn_print_info "Extracting pom.xml project version"

# The current version, checked explicitly since set -e doesn't apply to command substitution
pomVersion=$(./mvnw -B -q help:evaluate -Dexpression=project.version -DforceStdout) \
  || fn_fail "Unable to evaluate the project version"

[[ "${pomVersion}" =~ ^[0-9]+(\.[0-9]+)+-SNAPSHOT$ ]] \
  || fn_fail "Expected a snapshot version such as 26.1.0-SNAPSHOT, got '${pomVersion}'"

branch=$(git rev-parse --abbrev-ref HEAD)
[[ "${branch}" == "main" ]] || fn_fail "Releases are made from main, not '${branch}'"

# Version numbers come from the release tags rather than the POM, so that the first release
# of a year is YY.1.0 even when the POM still has a snapshot version of the previous year.
# Tag prefix as configured for the gitflow plugin in the release profile.
remoteTags=$(git ls-remote --tags --refs origin 'refs/tags/v*') \
  || fn_fail "Unable to list tags of origin"
remoteTags=$(sed 's|.*refs/tags/||' <<< "${remoteTags}")

# The latest release version from both local and remote tags, such as 26.1.0, or empty if none
latestRelease=$( { git tag -l 'v*'; echo "${remoteTags}"; } \
  | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' \
  | sed 's/^v//' \
  | sort -t . -n -k1,1 -k2,2 -k3,3 -u | tail -n 1)
IFS=. read -r latestYear latestN latestP <<< "${latestRelease}"

year=$(date +%y)

if [[ "${mode}" == "patch" ]]; then
  [[ -n "${latestRelease}" ]] || fn_fail "No release to patch"
  releaseVersion="${latestYear}.${latestN}.$(( latestP + 1 ))"
  # Main is already developing the next release, which stays the development version
  developmentVersion="${pomVersion}"
else
  n=1
  if [[ -n "${latestRelease}" ]]; then
    (( 10#${latestYear} <= 10#${year} )) \
      || fn_fail "Latest release ${latestRelease} is from a later year than ${year}, check the clock"
    if (( 10#${latestYear} == 10#${year} )); then
      n=$(( latestN + 1 ))
    fi
  fi
  releaseVersion="${year}.${n}.0"
  developmentVersion="${year}.$(( n + 1 )).0-SNAPSHOT"
fi

releaseTag="v${releaseVersion}"
if git rev-parse -q --verify "refs/tags/${releaseTag}" >/dev/null \
    || grep -qxF "${releaseTag}" <<< "${remoteTags}"; then
  fn_fail "Tag ${releaseTag} already exists"
fi

fn_print_info "Git branch: ${branch}"
fn_print_info "POM version is ${pomVersion}"
fn_print_info "Latest release: ${latestRelease:-none}"
fn_print_info "Release version: ${releaseVersion}, tag ${releaseTag}"
fn_print_info "Next development version: ${developmentVersion}"
fn_print_info "Distribution: target/battery-${releaseVersion}-bin.tar.gz"

echo -en "\n"

while true; do
    read -rp "Confirm releasing version '${releaseVersion}' of this project [y/N]" yn
    case "${yn}" in
        [Yy]* ) break;;
        [Nn]* ) echo Exiting; exit 1;;
        * ) echo "Please answer yes or no.";;
    esac
done

# The release profile declares and configures the gitflow plugin. It refuses to start when
# main is behind origin, and builds the distribution from the tagged release commit.
# Pushing the tag starts the release workflow, which publishes the distribution on GitHub.
./mvnw --batch-mode -Prelease gitflow:release \
    -DreleaseVersion="${releaseVersion}" \
    -DdevelopmentVersion="${developmentVersion}"

fn_print_info "Released ${releaseVersion} as tag ${releaseTag}, distribution in target/battery-${releaseVersion}-bin.tar.gz"
fn_print_info "GitHub release: https://github.com/kai-niemi/battery/releases/tag/${releaseTag}"
