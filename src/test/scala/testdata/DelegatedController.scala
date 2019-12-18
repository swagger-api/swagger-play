package testdata

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import play.api.mvc.ActionBuilder
import play.api.mvc.ControllerHelpers

@Api
class DelegatedController extends ControllerHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global
  private val Action = new ActionBuilder.IgnoringBody()

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
