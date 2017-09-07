package testdata

import javax.inject.Inject

import io.swagger.annotations._
import play.api.mvc.{InjectedController}
import play.mvc.{Http, Result}

@Api(value = "/apitest/document", description = "documents", tags = Array("Documents"))
class DocumentController  @Inject() () extends InjectedController {

  @ApiOperation(value = "Register acceptance of a file on a settlement",
    notes = "Accept file",
    httpMethod = "POST",
    nickname = "acceptsettlementfile",
    produces = "application/json")
  @ApiResponses(Array(
    new ApiResponse(code = Http.Status.BAD_REQUEST, message = "Bad Request"),
    new ApiResponse(code = Http.Status.UNAUTHORIZED, message = "Unauthorized"),
    new ApiResponse(code = Http.Status.INTERNAL_SERVER_ERROR, message = "Server error")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "Token for logged in user.", name = "Authorization", required = false, dataType = "string", paramType = "header")))
  def accept(@ApiParam(value = "Id of the settlement to accept a file on.") settlementId: String,
          @ApiParam(value = "File id of the file to accept.") fileId: String): Result = {
    play.mvc.Results.ok
  }
}