import play.modules.swagger._
import org.specs2.mutable._
import org.specs2.mock.Mockito
import scala.collection.JavaConversions._
import play.modules.swagger.util.SwaggerContext
import play.modules.swagger.routes.{ Route => PlayRoute }

class PlayApiScannerSpec extends Specification with Mockito {

  // set up mock for Play Router
  val routesList = {
    play.modules.swagger.routes.RoutesFileParser.parse("""
GET /api/dog test.testdata.DogController.list
PUT /api/dog test.testdata.DogController.add1
GET /api/cat @test.testdata.CatController.list
PUT /api/cat @test.testdata.CatController.add1
GET /api/fly test.testdata.FlyController.list
PUT /api/dog test.testdata.DogController.add1
PUT /api/dog/:id test.testdata.DogController.add0(id:String)
    """, "").right.get.collect {
      case (prefix, route: PlayRoute) => {
        val routeName = s"${route.call.packageName}.${route.call.controller}$$.${route.call.method}"
        (prefix, route)
      }
    }
  }

  val routesRules = Map(routesList map 
  { route =>
    {
      val routeName = s"${route._2.call.packageName}.${route._2.call.controller}$$.${route._2.call.method}"
      (routeName -> route._2)
    }
  } : _*)


  val route = new RouteWrapper(routesRules)
  RouteFactory.setRoute(route)

  "PlayApiScanner" should {
    "identify correct API classes based on router and API annotations" in {
      val classes = new PlayApiScanner().classes()
      
      classes.toList.length must beEqualTo(2)
      classes.find(clazz => clazz == SwaggerContext.loadClass("test.testdata.DogController")).nonEmpty must beTrue
      classes.find(clazz => clazz == SwaggerContext.loadClass("test.testdata.CatController")).nonEmpty must beTrue
    }
  }

}
