[![Build Status](https://travis-ci.org/rayyildiz/swagger-play.svg?branch=master)](https://travis-ci.org/rayyildiz/swagger-play)

# Swagger Play2 Module

## Overview
This is a module to support the play2 framework from [playframework](http://www.playframework.org).  It is written in scala but can be used with either java or scala-based play2 applications.

## Version History

* swagger-play2 1.6.1-SNAPSHOT supports play 2.6 and swagger 2.0.

* swagger-play2 1.5.1 supports play 2.4 and swagger 2.0.  If you need swagger 1.2 support, use 1.3.13. If you need 2.2 support, use 1.3.7 or earlier.

* swagger-play2 1.3.13 supports play 2.4.  If you need 2.2 support, use 1.3.7 or earlier.

* swagger-play2 1.3.12 supports play 2.3.  If you need 2.2 support, use 1.3.7 or earlier.

* swagger-play2 1.3.7 supports play 2.2.  If you need 2.1 support, please use 1.3.5 or earlier

* swagger-play2 1.3.6 requires play 2.2.x.

* swagger-play2 1.2.1 and greater support scala 2.10 and play 2.0 and 2.1.

* swagger-play2 1.2.0 support scala 2.9.x and play 2.0, please use 1.2.0.

Usage
-----

You can depend on pre-built libraries in maven central by adding the following dependency:

```
libraryDependencies ++= Seq(
  "io.swagger" %% "swagger-play2" % "1.6.1-SNAPSHOT"
)
```

Or you can build from source.

```
cd play-2.6/swagger-play2

sbt publishLocal
```

### Adding Swagger to your Play2 app

There are just a couple steps to integrate your Play2 app with swagger.

1\. Add the Swagger module to your `application.conf`
 
```
play.modules.enabled += "play.modules.swagger.SwaggerModule"
```
 
2\. Add the resource listing to your routes file (you can read more about the resource listing [here](https://github.com/swagger-api/swagger-core/wiki/Resource-Listing))

```

GET     /swagger.json           controllers.ApiHelpController.getResources

```

3\. Annotate your REST endpoints with Swagger annotations. This allows the Swagger framework to create the [api-declaration](https://github.com/swagger-api/swagger-core/wiki/API-Declaration) automatically!

In your controller for, say your "pet" resource:

```scala
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid ID supplied"),
    new ApiResponse(code = 404, message = "Pet not found")))
  def getPetById(
    @ApiParam(value = "ID of the pet to fetch") id: String) = Action {
    implicit request =>
      petData.getPetbyId(getLong(0, 100000, 0, id)) match {
        case Some(pet) => JsonResponse(pet)
        case _ => JsonResponse(new value.ApiResponse(404, "Pet not found"), 404)
      }
  }

```

What this does is the following:

* Tells swagger that the methods in this controller should be described under the `/api-docs/pet` path

* The Routes file tells swagger that this API listens to `/{id}`

* Describes the operation as a `GET` with the documentation `Find pet by Id` with more detailed notes `Returns a pet ....`

* Takes the param `id`, which is a datatype `string` and a `path` param

* Returns error codes 400 and 404, with the messages provided

In the routes file, you then wire this api as follows:

```
GET     /pet/:id                 controllers.PetApiController.getPetById(id)
```

This will "attach" the /api-docs/pet api to the swagger resource listing, and the method to the `getPetById` method above

Please note that the minimum configuration needed to have a route/controller be exposed in swagger declaration is to have an `Api` annotation at class level.

#### The ApiParam annotation

Swagger for play has two types of `ApiParam`s--they are `ApiParam` and `ApiImplicitParam`.  The distinction is that some
paramaters (variables) are passed to the method implicitly by the framework.  ALL body parameters need to be described
with `ApiImplicitParam` annotations.  If they are `queryParam`s or `pathParam`s, you can use `ApiParam` annotations.


# application.conf - config options
```
api.version (String) - version of API | default: "beta"
swagger.api.basepath (String) - base url | default: "http://localhost:9000"
swagger.filter (String) - classname of swagger filter | default: empty
swagger.api.info = {
  contact : (String) - Contact Information | default : empty,
  description : (String) - Description | default : empty,
  title : (String) - Title | default : empty,
  termsOfService : (String) - Terms Of Service | default : empty,
  license : (String) - Terms Of Service | default : empty,
  licenseUrl : (String) - Terms Of Service | default : empty
}
```
## Rendering SecurityDefinition

To render SecurityDefinition you need to add *SecurityDefinition* in *SwaggerDefinition* and *Authorization* to method that will require Authorization
```
@Singleton
@Api(value = "Customer")
@SwaggerDefinition(
  securityDefinition = new SecurityDefinition(
    apiKeyAuthDefintions = Array(
      new ApiKeyAuthDefinition(
        name = "Authorization",
        key = "Bearer",
        in = ApiKeyAuthDefinition.ApiKeyLocation.HEADER,
        description="For Accessing the API must provide a valid JWT Token ")
    )
  )
)
class CustomerController @Inject() (val controllerComponents: ControllerComponents) extends BaseController  {


  @ApiOperation(value = "get All Customers",
    nickname = "getAllCustomers",
    notes = "Retuns a list of Customer",
    response = classOf[Customer],
    responseContainer = "List",
    httpMethod = "GET",
    authorizations = Array(
      new Authorization("Bearer")
    )
  )
```
By annotating coding as shown above swagger.json will contains necessary elementos to be used with swagger-ui
```
    "/customers" : {
      "get" : {
        "tags" : [ "Customer" ],
        "summary" : "get All Customers",
        "description" : "Retuns a list of Customer",
        "operationId" : "getAllCustomers",
        "parameters" : [ ],
        "responses" : {
          "200" : {
            "description" : "successful operation",
            "schema" : {
              "type" : "array",
              "items" : {
                "$ref" : "#/definitions/Customer"
              }
            }
          }
        },
        "security" : [ {
          "Bearer" : [ ]
        } ]
      }
    }
  },
  "securityDefinitions" : {
    "Bearer" : {
      "description" : "For Accessing the API must provide a valid JWT Token ",
      "type" : "apiKey",
      "name" : "Authorization",
      "in" : "header"
    }
  },
```

A great explanation about *Use Authorization Header with Swagger* can be found here http://www.mimiz.fr/blog/use-authorization-header-with-swagger/

## Note on Dependency Injection
This plugin works by default if your application uses Runtime dependency injection.

Nevertheless, a helper is provided `SwaggerApplicationLoader` to ease the use of this plugin with Compile Time Dependency Injection. 


## License

```
Copyright 2017 SmartBear Software

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
