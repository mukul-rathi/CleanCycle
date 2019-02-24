#!/bin/bash

gradle build 

docker-compose -f database-server/tests.yml build
docker-compose -f database-server/tests.yml run test


