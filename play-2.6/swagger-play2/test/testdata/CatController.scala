package testdata

import io.swagger.annotations._

import play.api.mvc.{Action, Controller}

@SwaggerDefinition(
  securityDefinition = new SecurityDefinition(
    apiKeyAuthDefinitions = Array(
      new ApiKeyAuthDefinition(name= "api_key", key="api_key", in=ApiKeyAuthDefinition.ApiKeyLocation.HEADER)
    ),
    oAuth2Definitions = Array(
      new OAuth2Definition(
        key = "oauth2",
        flow = OAuth2Definition.Flow.APPLICATION,
        tokenUrl= "/oauth/token",
        authorizationUrl = "/authorize",
        scopes = Array(new Scope(name = "write_pets", description = "modify pets"))
      )
    ),
    basicAuthDefinitions = Array(new BasicAuthDefinition(key = "basic_auth"))
  )
)
@Api(value = "/apitest/cats", description = "play with cats", authorizations = Array (
  new Authorization(
    value = "oauth2"
  ),
  new Authorization(
    value = "api_key"
  ),
  new Authorization(
    value = "basic_auth"
  )
))
class CatController extends Controller {

  @ApiOperation(value = "addCat1",
      httpMethod = "PUT",
      authorizations = Array(new Authorization(value="api_key")),
      consumes = "",
      protocols = "")
    @ApiImplicitParams(Array(
      new ApiImplicitParam(name = "cat", value = "Cat object to add", required = true, dataType = "testdata.Cat", paramType = "body")))
    def add1 = Action {
      request => Ok("test case")
    }

    @ApiOperation(value = "Updates a new Cat",
      notes = "Updates cats nicely",
      httpMethod = "POST")
    @ApiResponses(Array(
      new ApiResponse(code = 405, message = "Invalid input")))
    @ApiImplicitParams(Array(
      new ApiImplicitParam(name = "cat", value = "Cat object to update", required = true, dataType = "testdata.Cat", paramType = "body")))
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
      httpMethod = "GET",
      authorizations = Array(new Authorization(value="oauth2", scopes = Array(new AuthorizationScope(scope = "write_pets", description = "Modify pets")))))
    @Deprecated
    def list = Action {
      request => Ok("test case")
    }

    def no_route = Action {
      request => Ok("test case")
    }

  @ApiOperation(value = "test issue #43",
    nickname = "test issue #43_nick",
    notes = "test issue #43_notes",
    response = classOf[Cat],
    responseContainer = "List",
    httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "test_issue_43_implicit_param", dataType = "Option[Int]", value = "test issue #43 implicit param", paramType = "query")))
  def testIssue43(test_issue_43_param:  Option[Int]) = Action {
    request => Ok("test issue #43")
  }
}

case class Cat(id: Long, name: String)
