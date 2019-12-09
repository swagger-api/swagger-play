[![Build Status](https://travis-ci.org/swagger-api/swagger-play.svg?branch=master)](https://travis-ci.org/swagger-api/swagger-play)

# Swagger Play2 Module

## Overview
This is a module to support Swagger annotations within [Play Framework](http://www.playframework.org) controllers. This library uses Swagger 1.5 and supports Play 2.6 (version 1.0.x) and Play 2.7 (version 2.0.x). It can be used for both Scala and Java based applications.

We also would like to support Swagger 2.0 in the future and contributions to that end will be gladly accepted.

## Version History

* swagger-play2 2.0.1-SNAPSHOT supports play 2.6/2.7 and swagger 2.0. Merged from fork https://github.com/Iterable/swagger-play

###New and Noteworthy
 
 - Minimal dependencies: only depends on the core Play module, so it won't bring unnecessary dependencies on the Akka HTTP server or anything else from Play.
 - `SwaggerPlugin` no longer depends on on `Application`.
 - Correct `Content-Length` generation for JSON (originally proposed in https://github.com/swagger-api/swagger-play/pull/176)
 - No longer uses deprecated Play configuration methods (proposed in https://github.com/swagger-api/swagger-play/pull/162). Also uses `reference.conf` for default values.
 - Clarifies compile-time DI docs (proposed in https://github.com/swagger-api/swagger-play/pull/157)
 - Handle route delegation properly (https://github.com/swagger-api/swagger-play/pull/132 updated for Play 2.6)
 - Add support for `dataTypeClass` in `ApiImplicitParam` (https://github.com/swagger-api/swagger-play/pull/174)
 - Add support for API keys (https://github.com/swagger-api/swagger-play/pull/117)
 - Add support for OAuth2 (https://github.com/swagger-api/swagger-play/pull/183)

* swagger-play2 1.7.1 supports play 2.7 and swagger 2.0.

* swagger-play2 1.7.0 supports play 2.7 and swagger 2.0.

* swagger-play2 1.6.1 supports play 2.6 and swagger 2.0.

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
  "io.swagger" %% "swagger-play2" % "2.0.1"
)
```

Or you can build from source.

```
cd play-2.7/swagger-play2

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

## Note on Dependency Injection
This plugin works by default if your application uses Runtime dependency injection.

Nevertheless, the plugin can be initialized using compile time dependency injection. For example, you can add the following to your class that extends `BuiltInComponentsFromContext`:
```
// This needs to be eagerly instantiated because this sets global state for swagger
val swaggerPlugin = new SwaggerPluginImpl(environment, configuration)
lazy val apiHelpController = new ApiHelpController(controllerComponents, swaggerPlugin)
```


## License

```
Copyright 2019 SmartBear Software

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
