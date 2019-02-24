#!/bin/bash

gradle build 

docker-compose -f database-server/docker-compose.yml build
docker-compose -f database-server/docker-compose.yml up  
