package test.testdata

import io.swagger.annotations._

import play.api.mvc.{Action, Controller}

@Api(value = "/apitest/cats", description = "play with cats")
class CatController extends Controller {

  @ApiOperation(value = "addCat1",
      httpMethod = "PUT",
      authorizations = Array(),
      consumes = "",
      protocols = "")
    @ApiImplicitParams(Array(
      new ApiImplicitParam(name = "cat", value = "Cat object to add", required = true, dataType = "test.testdata.Cat", paramType = "body")))
    def add1 = Action {
      request => Ok("test case")
    }

    @ApiOperation(value = "Updates a new Cat",
      notes = "Updates cats nicely",
      httpMethod = "POST")
    @ApiResponses(Array(
      new ApiResponse(code = 405, message = "Invalid input")))
    @ApiImplicitParams(Array(
      new ApiImplicitParam(name = "cat", value = "Cat object to update", required = true, dataType = "test.testdata.Cat", paramType = "body")))
    def update = Action {
      request => Ok("test case")
    }

    @ApiOperation(value = "Get Cat by Id",
      notes = "Returns a cat",
      response = classOf[Cat],
      httpMethod = "GET",
      produces = "")
    @ApiResponses(Array(
      new ApiResponse(code = 405, message = "Invalid input"),
      new ApiResponse(code = 404, message = "Cat not found")))
    def get1(@ApiParam(value = "ID of cat to fetch", required = true) id: Long) = Action {
      request => Ok("test case")
    }

    @ApiOperation(value = "List Cats",
      nickname = "listCats",
      notes = "Returns all cats",
      response = classOf[Cat],
      responseContainer = "List",
      httpMethod = "GET")
    @Deprecated
    def list = Action {
      request => Ok("test case")
    }

    def no_route = Action {
      request => Ok("test case")
    }
}

case class Cat(id: Long, name: String)
