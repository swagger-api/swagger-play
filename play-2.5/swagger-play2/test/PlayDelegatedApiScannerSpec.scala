import io.swagger.config.ScannerFactory
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.specification.BeforeAfterAll
import play.modules.swagger._
import play.routes.compiler.Route

import scala.collection.JavaConverters._

class PlayDelegatedApiScannerSpec extends Specification with Mockito with BeforeAfterAll {

  val routes: List[Route] =
    SwaggerPluginHelper.parseRoutes("delegation", "", _ => {}, Thread.currentThread().getContextClassLoader)


  val routesRules: Map[String, Route] = Map(routes.map { route: Route =>
    s"${route.call.packageName}.${route.call.controller}$$.${route.call.method}" -> route
  }: _*)


  val swaggerConfig = new PlaySwaggerConfig()
  swaggerConfig.setBasePath("")
  swaggerConfig.setHost("127.0.0.1")

  override def afterAll(): Unit = {}

  override def beforeAll(): Unit = {
    ApiListingCache.cache = None
    PlayConfigFactory.setConfig(swaggerConfig)
    ScannerFactory.setScanner(new PlayApiScanner())
    RouteFactory.setRoute(new RouteWrapper(routesRules.asJava))
  }


  "route parsing" should {
    "separate delegated paths correctly" in {

      val urls = ApiListingCache.listing("", "127.0.0.1").get.getPaths.keySet().asScala

      urls must contain("/api/all")
      urls must contain("/api/my/action")
      urls must contain("/api/subdelegated/all")
      urls must contain("/api/subdelegated/my/action")
      urls must contain("/api/subdelegated")
    }
  }

}
