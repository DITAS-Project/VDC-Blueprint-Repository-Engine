#!/usr/bin/env bash
# Production environment: 178.22.69.83
# Private key for ssh: /opt/keypairs/ditas-testbed-keypair.pem

# This file is part of VDC-Blueprint-Repository-Engine.
# 
# VDC-Blueprint-Repository-Engine is free software: you can redistribute it 
# and/or modify it under the terms of the GNU General Public License as 
# published by the Free Software Foundation, either version 3 of the License, 
# or (at your option) any later version.
# 
# VDC-Blueprint-Repository-Engine is distributed in the hope that it will be 
# useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with VDC-Blueprint-Repository-Engine.  
# If not, see <https://www.gnu.org/licenses/>.
# 
# VDC-Blueprint-Repository-Engine is being developed for the
# DITAS Project: https://www.ditas-project.eu/


# TODO state management? We are killing without careing about any operation the conainer could be doing.

ssh -i /opt/keypairs/ditas-testbed-keypair.pem cloudsigma@178.22.69.83 << 'ENDSSH'

# Ensure that a previously running instance is stopped (-f stops and removes in a single step)
# || true - "docker stop" failt with exit status 1 if image doen't exists, what makes the Pipeline fail. the "|| true" forces the command to exit with 0.
sudo docker stop --time 20 vdc-blueprint-repository-engine || true
sudo docker rm --force vdc-blueprint-repository-engine || true
sudo docker pull ditas/vdc-blueprint-repository-engine:latest

# Get the host IP
HOST_IP="$(ip route get 8.8.8.8 | awk '{print $NF; exit}')"



# SET THE PORT MAPPING and pass the host IP via the environmental variable "DOCKER_HOST_IP"
sudo docker run -p 50009:8080 -e DOCKER_HOST_IP=$HOST_IP --restart unless-stopped -d --name vdc-blueprint-repository-engine ditas/vdc-blueprint-repository-engine:latest
ENDSSH
