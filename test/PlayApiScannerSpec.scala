import java.io.File

import play.modules.swagger._
import org.specs2.mutable._
import org.specs2.mock.Mockito
import scala.collection.JavaConversions._
import play.modules.swagger.util.SwaggerContext
import play.routes.compiler.{ Route => PlayRoute }

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
      case (route: PlayRoute) => {
        val routeName = s"${route.call.packageName}.${route.call.controller}$$.${route.call.method}"
        route
      }
    }
  }

  val routesRules = Map(routesList map 
  { route =>
    {
      val routeName = s"${route.call.packageName}.${route.call.controller}$$.${route.call.method}"
      routeName -> route
    }
  } : _*)


  val route = new RouteWrapper(routesRules)
  RouteFactory.setRoute(route)

  "PlayApiScanner" should {
    "identify correct API classes based on router and API annotations" in {
      val classes = new PlayApiScanner().classes()
      
      classes.toList.length must beEqualTo(2)
      classes.contains(SwaggerContext.loadClass("testdata.DogController")) must beTrue
      classes.contains(SwaggerContext.loadClass("testdata.CatController")) must beTrue
    }
  }

}
