# VDC Blueprint Repository Engine
The goal of the component is to provide CRUD operations to other DITAS components and DITAS roles via a HTTP REST interface.

## List of functionalities
* `GET` `/ditas/blueprints`
  * **description**: this method retrieves all blueprints (or specific sections).
  * (indicative) **caller** Resolution Engine
  * **input**
    * sections to be projected _(optional)_
    * filters to be applied on the returned documents _(optional)_
  * **output**
    * list of blueprints 

* `GET` `/ditas/blueprints/{id}`
  * **description**: this method retrieves a blueprint (or specific sections) based on its id. 
  * (indicative) **caller** Resolution Engine
  * **input**
    * blueprint's id
    * sections to be projected _(optional)_
  * **output**
    * blueprint

* `POST` `/ditas/blueprints`
  * **description**: this method creates a new blueprint.
  * **caller** Data Administrator
  * **input**
    * abstract blueprint(s) to be stored in the VDC Blueprint Repository
  * **output**
    * the id(s) of the created blueprint(s)

* `PATCH` `/ditas/blueprints/{id}`
  * **description**: this method updates an existing blueprint based by its id.
  * **caller** Data Administrator
  * **input**
    * blueprint's id
    * all the fields to be updated
  * **output**
    * none

* `DELETE` `/ditas/blueprints/{id}`
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

## Execution
Application requires two configuration files : 
configuration.yml 
security.yml

To launch this component, execute the following command:
```
$ java -jar target/vdc-repository-engine etc/configuration.yml
```
