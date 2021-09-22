#!/bin/bash
set -eu
cd `dirname $0`

cp ../bukkit/target/build/itemstack-count.jar .
docker-compose build
docker-compose up -d
