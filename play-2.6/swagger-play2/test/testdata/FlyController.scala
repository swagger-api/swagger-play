package testdata

import javax.inject.Inject

import play.api.mvc.{InjectedController}

class FlyController @Inject() () extends InjectedController  {

  def list = Action {
    request =>
      Ok("test case")
  }

}

case class Fly(id: Long, name: String)
