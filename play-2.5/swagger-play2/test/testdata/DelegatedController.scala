package testdata

import io.swagger.annotations.{Api, ApiOperation}
import play.api.mvc.{Action, Controller}

@Api
object DelegatedController extends Controller {

  @ApiOperation(value = "list")
  def list = Action { _ => Ok("test case")}

  @ApiOperation(value = "list2")
  def list2 = Action { _ => Ok("test case")}

  @ApiOperation(value = "list3")
  def list3 = Action { _ => Ok("test case")}

  @ApiOperation(value = "list4")
  def list4 = Action { _ => Ok("test case")}

  @ApiOperation(value = "list5")
  def list5 = Action { _ => Ok("test case")}

}
