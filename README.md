# VDC Blueprint Repository Engine
The goal of the component is to provide CRUD operations to other DITAS components and DITAS roles via a HTTP REST interface.

## License
This file is part of VDC-Blueprint-Repository-Engine.

VDC-Blueprint-Repository-Engine is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as 
published by the Free Software Foundation, either version 3 of the License, 
or (at your option) any later version.

VDC-Blueprint-Repository-Engine is distributed in the hope that it will be 
useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with VDC-Blueprint-Repository-Engine.  
If not, see <https://www.gnu.org/licenses/>.

VDC-Blueprint-Repository-Engine is being developed for the
DITAS Project: https://www.ditas-project.eu/


## List of functionalities
* `GET` `/blueprints`
  * **description**: this method retrieves all blueprints (or specific sections).
  * (indicative) **caller** Resolution Engine
  * **input**
    * sections to be projected _(optional)_
    * filters to be applied on the returned documents _(optional)_
  * **output**
    * list of blueprints 

* `GET` `/blueprints/{id}`
  * **description**: this method retrieves a blueprint (or specific sections) based on its id. 
  * (indicative) **caller** Resolution Engine
  * **input**
    * blueprint's id
    * sections to be projected _(optional)_
  * **output**
    * blueprint

* `POST` `/blueprints`
  * **description**: this method creates a new blueprint.
  * **caller** Data Administrator
  * **input**
    * abstract blueprint(s) to be stored in the VDC Blueprint Repository
  * **output**
    * the id(s) of the created blueprint(s)

* `PATCH` `/blueprints/{id}`
  * **description**: this method updates an existing blueprint based by its id.
  * **caller** Data Administrator
  * **input**
    * blueprint's id
    * all the fields to be updated
  * **output**
    * none

* `DELETE` `/blueprints/{id}`
  * **description**: this method deletes an existing blueprint based by its id.
  * **caller** Data Administrator
  * **input**
    * blueprint's id
  * **output**
    * none

## API definition
API definition in [SwaggerHub](https://app.swaggerhub.com/apis/ditas-iccs/VDC-Blueprint-Repository-Engine/0.0.1).

## Implementation language
Java

## Requirements
In order to work, this component requires the following elements to be installed:

* jre 8

## Installation
* clone this repository.

## Execution
Application requires two configuration files : 
* configuration.yml 
* security.yml

To launch this component, execute the following command:
```
$ java -jar target/vdc-repository-engine-0.0.1-SNAPSHOT.jar etc/configuration.yml
```