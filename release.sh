#!/bin/bash
# Maven+gitflow release script

set -e

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
  || fn_fail "Expected a snapshot version such as 1.2.3-SNAPSHOT, got '${pomVersion}'"

# The version to be released, without the snapshot suffix
releaseVersion="${pomVersion%-SNAPSHOT}"
# The next development version, with the last number advanced by one
developmentVersion="${releaseVersion%.*}.$(( ${releaseVersion##*.} + 1 ))-SNAPSHOT"

branch=$(git rev-parse --abbrev-ref HEAD)
[[ "${branch}" == "main" ]] || fn_fail "Releases are made from main, not '${branch}'"

# Tag prefix as configured for the gitflow plugin in the release profile
releaseTag="v${releaseVersion}"
remoteTag=$(git ls-remote --tags origin "refs/tags/${releaseTag}") \
  || fn_fail "Unable to list tags of origin"
if git rev-parse -q --verify "refs/tags/${releaseTag}" >/dev/null || [[ -n "${remoteTag}" ]]; then
  fn_fail "Tag ${releaseTag} already exists"
fi

fn_print_info "Git branch: ${branch}"
fn_print_info "POM version is ${pomVersion}"
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
