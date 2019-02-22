#!/bin/bash

gradle build 


if [ -f "./database-server/docker-compose.yml" ] 
    then 
    echo "POSTGRES_DB=testdb POSTGRES_USER=test 
POSTGRES_PASSWORD=test" >> database-server/database.env
    docker-compose -f database-server/docker-compose.yml build
    docker-compose -f database-server/docker-compose.yml up

        
else
    echo "No Docker Compose file"
fi 