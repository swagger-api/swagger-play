package testdata

import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents

class FlyController(override val controllerComponents: ControllerComponents) extends BaseController {

  def list = Action {
    request =>
      Ok("test case")
  }

}

case class Fly(id: Long, name: String)
