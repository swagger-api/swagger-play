package testdata

import io.swagger.annotations._
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.mvc.Http
import play.mvc.Result

@Api(value = "/apitest/pointsofinterest", description = "Points of interest")
class PointOfInterestController(override val controllerComponents: ControllerComponents) extends BaseController {

  @ApiOperation(value = "Get points of interest",
    notes = "Returns points of interest",
    httpMethod = "GET",
    nickname = "pointsofinterest",
    produces = "application/json")
  @ApiResponses(Array(
    new ApiResponse(code = Http.Status.BAD_REQUEST, message = "Bad Request"),
    new ApiResponse(code = Http.Status.UNAUTHORIZED, message = "Unauthorized"),
    new ApiResponse(code = Http.Status.INTERNAL_SERVER_ERROR, message = "Server error")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "Token for logged in user.", name = "Authorization", required = false, dataType = "string", paramType = "header")))
  def list(@ApiParam(value = "Minimum easting for provided extent", required = true, defaultValue = "-19448.67") eastingMin: Double,
          @ApiParam(value = "Minimum northing for provided extent", required = true, defaultValue = "2779504.82") northingMin: Double,
          @ApiParam(value = "Maximum easting for provided extent", required = true, defaultValue = "-17557.26") eastingMax: Double,
          @ApiParam(value = "Maximum northing for provided extent", required = true, defaultValue = "2782860.09") northingMax: Double): Result = {
    play.mvc.Results.ok
  }
}
