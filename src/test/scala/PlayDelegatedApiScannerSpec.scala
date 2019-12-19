import io.swagger.config.ScannerFactory
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.specification.BeforeAfterAll
import play.api.Environment
import play.modules.swagger._
import play.routes.compiler.Route

import scala.jdk.CollectionConverters._

class PlayDelegatedApiScannerSpec extends Specification with Mockito with BeforeAfterAll {

  val routes: List[Route] =
    SwaggerPluginHelper.parseRoutes("delegation", "/api", Environment.simple())

  val routesRules: Map[String, Route] = Map(routes.map { route: Route =>
    (route.call.packageName.toSeq ++ Seq(route.call.controller + "$", route.call.method)).mkString(".") -> route
  }: _*)

  val swaggerConfig = PlaySwaggerConfig.defaultReference.copy(
    basePath = "",
    host = "127.0.0.1"
  )

  val env = Environment.simple()
  val scanner = new PlayApiScanner(swaggerConfig, new RouteWrapper(routesRules), env)
  val route = new RouteWrapper(routesRules)
  val playReader = new PlayReader(swaggerConfig, route, null)
  val apiListingCache = new ApiListingCache(scanner, playReader)

  override def afterAll(): Unit = {}

  override def beforeAll(): Unit = {
    ScannerFactory.setScanner(scanner)
  }


  "route parsing" should {
    "separate delegated paths correctly" in {

      val urls = apiListingCache.listing("127.0.0.1").getPaths.keySet.asScala

      urls must contain("/api/all")
      urls must contain("/api/my/action")
      urls must contain("/api/subdelegated/all")
      urls must contain("/api/subdelegated/my/action")
      urls must contain("/api/subdelegated")
    }
  }

}
