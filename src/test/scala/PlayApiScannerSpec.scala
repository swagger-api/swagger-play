import java.io.File

import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.api.Environment
import play.modules.swagger._
import play.routes.compiler.{Route => PlayRoute}

import scala.jdk.CollectionConverters._

class PlayApiScannerSpec extends Specification with Mockito {

  // set up mock for Play Router
  val routesList = {
    play.routes.compiler.RoutesFileParser.parseContent("""
GET /api/dog testdata.DogController.list
PUT /api/dog testdata.DogController.add1
GET /api/cat @testdata.CatController.list
PUT /api/cat @testdata.CatController.add1
GET /api/fly testdata.FlyController.list
PUT /api/dog testdata.DogController.add1
PUT /api/dog/:id testdata.DogController.add0(id:String)
                                                       """, new File("")).right.get.collect {
      case route: PlayRoute => route
    }
  }

  val routesRules = Map(routesList map { route =>
    val routeName = s"${route.call.packageName}.${route.call.controller}$$.${route.call.method}"
    routeName -> route
  } : _*)

  val route = new RouteWrapper(routesRules)
  val env = Environment.simple()

  "PlayApiScanner" should {
    "identify correct API classes based on router and API annotations" in {
      val classes = new PlayApiScanner(PlaySwaggerConfig.defaultReference, route, env).classes()

      classes.asScala must haveSize(2)
      classes.contains(env.classLoader.loadClass("testdata.DogController")) must beTrue
      classes.contains(env.classLoader.loadClass("testdata.CatController")) must beTrue
    }
  }

}
