[![Build Status](https://travis-ci.org/swagger-api/swagger-play.svg?branch=master)](https://travis-ci.org/swagger-api/swagger-play)

# Swagger Play2 Module

## Overview
This is a module to support the play2 framework from [playframework](http://www.playframework.org).  It is written in scala but can be used with either java or scala-based play2 applications.

## Swagger Samples
For an example of using `swagger-play` with Play 2.4, see [swagger-samples](https://github.com/swagger-api/swagger-samples).

## Version Support

* swagger-play2 1.5.3 supports play 2.5 and swagger 2.0.  

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
  "io.swagger" %% "swagger-play2" % "1.5.1"
)
```

Or you can build from source.

```
cd modules/swagger-play2

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

Nevertheless, a helper is provided `SwaggerApplicationLoader` to ease the use of this plugin with Compile Time Dependency Injection. 
