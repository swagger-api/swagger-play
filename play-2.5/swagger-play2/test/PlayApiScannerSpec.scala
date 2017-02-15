import java.io.File

import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.specification.BeforeAfterAll
import play.modules.swagger._
import play.modules.swagger.util.SwaggerContext
import play.routes.compiler.{Route => PlayRoute}

import scala.collection.JavaConversions._

class PlayApiScannerSpec extends Specification with Mockito with BeforeAfterAll {

  // set up mock for Play Router
  val routesList: List[PlayRoute] =
    play.routes.compiler.RoutesFileParser.parseContent(
"""
GET /api/dog testdata.DogController.list
PUT /api/dog testdata.DogController.add1
GET /api/cat @testdata.CatController.list
PUT /api/cat @testdata.CatController.add1
GET /api/fly testdata.FlyController.list
PUT /api/dog testdata.DogController.add1
PUT /api/dog/:id testdata.DogController.add0(id:String)
""", new File("")).right.get.collect { case (route: PlayRoute) => route}


  val routesRules = Map(routesList map { route =>
      s"${route.call.packageName}.${route.call.controller}$$.${route.call.method}" -> route
  }: _*)


  override def afterAll(): Unit = {}

  override def beforeAll(): Unit = {
    RouteFactory.setRoute(new RouteWrapper(routesRules))
  }


  "PlayApiScanner" should {
    "identify correct API classes based on router and API annotations" in {
      val classes = new PlayApiScanner().classes()

      classes.toList.length must beEqualTo(2)
      classes.contains(SwaggerContext.loadClass("testdata.DogController")) must beTrue
      classes.contains(SwaggerContext.loadClass("testdata.CatController")) must beTrue
    }
  }

}
