# Swagger Play 2.8.x Module

![build](https://github.com/kinoplan/swagger-play/workflows/build/badge.svg)

## Overview

This is a module to support Swagger within [Play Framework](http://www.playframework.org) controllers. It is based on the library https://github.com/swagger-api/swagger-play with several improvements. This library uses Swagger Core 2.X and Play 2.8. It can be used for both Scala and Java based applications.

## Usage

You can depend on pre-built libraries in maven central by adding the following dependency:

```
libraryDependencies ++= Seq(
  "io.kinoplan" %% "swagger-play" % "{version}"
)
```

Or you can build from source.

```
sbt publishLocal
```

### Adding Swagger to your Play2 app

There are just a couple steps to integrate your Play2 app with swagger.

1\. Add the Swagger module to your `application.conf`
 
```
play.modules.enabled += "play.modules.swagger.SwaggerModule"
```
 
2\. Add the resource listing to your routes file

```

GET     /swagger.json           controllers.ApiHelpController.getResources

```

3\. Annotate your REST endpoints with [Open API Annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations#open-api-specification-annotations). This allows the Swagger framework to create the [OpenAPI Specification](https://github.com/OAI/OpenAPI-Specification/blob/master/README.md) automatically!

In your controller for, say your "pet" resource:

```scala
  @Operation(
    tags = Array("Pets"),
    summary = "Find pet by Id",
    description = "Returns a pet",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Pet",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[PetDTO]))
        )
      ),
      new ApiResponse(responseCode = "404", description = "Pet not found")
    )
  )
  def getPetById(
    @Parameter(
    required = true,
    name = "id",
    in = ParameterIn.PATH,
    description = "ID of a pet",
    schema = new Schema(implementation = classOf[Int], example = "45124")
    )
    id: Long
  ): Action[AnyContent] = auth.async { implicit request =>
  
    Ok(Json.toJson(petData.getById(id)))  
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


## application.conf - config options
```
swagger.api.version (String) - version of API | default: "beta"
swagger.api.basepath (String) - base url | default: ""
swagger.api.host (string) - host | default: "localhost:9000"
swagger.api.info = {
  contact = {
    name: (String) - Name | default: empty,
    url: (String) - Url | default: empty,
    email: (String) - email | default: empty
  }
  description: (String) - Description | default: empty,
  title: (String) - Title | default: empty,
  termsOfService: (String) - Terms Of Service | default: empty,
  license = {
    name: (String) - Name | default: empty,
    url: (String) - Url | default: empty
  }
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

## Contributing

See [CONTRIBUTING.md](/CONTRIBUTING.md) for more details about how to contribute.

## License

This project is licensed under the terms of the [Apache License, Version 2.0](/LICENSE).

## Security contact

Please disclose any security-related issues or vulnerabilities by emailing [security@swagger.io](mailto:security@swagger.io), instead of using the public issue tracker.
