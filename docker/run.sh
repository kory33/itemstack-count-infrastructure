#!/bin/bash
set -eu
cd `dirname $0`

# Run
mkdir -p data
echo 'eula=true' > data/eula.txt
docker-compose up
