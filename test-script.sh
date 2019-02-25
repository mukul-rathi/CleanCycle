#!/bin/bash

gradle build 

docker-compose -f database-server/docker-tests.yml build
docker-compose -f database-server/docker-tests.yml run test


