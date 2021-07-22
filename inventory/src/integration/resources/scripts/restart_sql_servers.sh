#!/bin/bash

docker-compose -f docker-compose/db.yml down
docker-compose -f docker-compose/db.yml up -d
sleep 25
