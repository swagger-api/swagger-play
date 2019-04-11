package testdata

import play.api.mvc.InjectedController

object FlyController extends InjectedController {

  def list = Action {
    request =>
      Ok("test case")
  }

}

case class Fly(id: Long, name: String)
