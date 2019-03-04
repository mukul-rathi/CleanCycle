#!/usr/bin/env bash
#This script boots up the analytics code in the docker container

if [[ -z "$TEST_ENV" ]]; then #checks if TEST_ENV enivronment variable not set 
  gradle assemble #deploy to production
  gradle run
else
   gradle build #run tests
fi