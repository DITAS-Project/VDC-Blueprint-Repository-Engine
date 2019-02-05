#!/bin/sh

# Find the line host_ip: X.X.X.X and replace it with the docker host ip on the yml file
sed -i -e 's/X.X.X.X/'$HOST_IP'/g' /app/configuration.yml

# Run the application
java -jar /app/vdc-repository-engine-0.0.1-SNAPSHOT.jar /app/configuration.yml
