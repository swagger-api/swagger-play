import io.swagger.config.{ ScannerFactory }
import io.swagger.models.{ModelImpl, HttpMethod}
import io.swagger.models.parameters.{BodyParameter, PathParameter}
import io.swagger.models.properties.{RefProperty, ArrayProperty}
import play.modules.swagger._
import org.specs2.mutable._
import org.specs2.mock.Mockito
import scala.Some
import play.api.Logger
import io.swagger.util.Json
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import play.modules.swagger.routes.{ Route => PlayRoute }

class PlayApiListingCacheSpec extends Specification with Mockito {

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

  val apiVersion = "test1"
  val basePath = "/api"

  var swaggerConfig = new PlaySwaggerConfig()

  swaggerConfig setDescription "description"
  swaggerConfig setBasePath basePath
  swaggerConfig setContact "contact"
  swaggerConfig setHost "127.0.0.1"
  swaggerConfig setVersion "beta"
  swaggerConfig setTitle "title"
  swaggerConfig setTermsOfServiceUrl "http://termsOfServiceUrl"
  swaggerConfig setLicense "license"
  swaggerConfig setLicenseUrl "http://licenseUrl"

  PlayConfigFactory.setConfig(swaggerConfig)

  var scanner = new PlayApiScanner()
  ScannerFactory.setScanner(scanner)
  val route = new RouteWrapper(routesRules)
  RouteFactory.setRoute(route)

  "ApiListingCache" should {

    "load all API specs" in {

      val docRoot = ""
      val swagger = ApiListingCache.listing(docRoot, "127.0.0.1")

      Logger.debug ("swagger: " + toJsonString(swagger.get))
      swagger must beSome
      swagger.get.getSwagger must beEqualTo("2.0")
      swagger.get.getBasePath must beEqualTo(basePath)
      swagger.get.getPaths.size must beEqualTo(3)
      swagger.get.getDefinitions.size must beEqualTo(3)
      swagger.get.getHost must beEqualTo(swaggerConfig.getHost)
      swagger.get.getInfo.getContact.getName must beEqualTo(swaggerConfig.getContact)
      swagger.get.getInfo.getVersion must beEqualTo(swaggerConfig.getVersion)
      swagger.get.getInfo.getTitle must beEqualTo(swaggerConfig.getTitle)
      swagger.get.getInfo.getTermsOfService must beEqualTo(swaggerConfig.getTermsOfServiceUrl)
      swagger.get.getInfo.getLicense.getName must beEqualTo(swaggerConfig.getLicense)

      val pathCat = swagger.get.getPaths().get("/cat")
      pathCat.getOperations.size must beEqualTo(2)

      val opCatGet = pathCat.getOperationMap.get(HttpMethod.GET)
      opCatGet.getOperationId must beEqualTo("listCats")
      opCatGet.getParameters must beEmpty
      opCatGet.getConsumes must beNull
      opCatGet.getResponses.get("200").getSchema.asInstanceOf[ArrayProperty].getItems.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("Cat")
      opCatGet.getProduces must beNull

      val opCatPut = pathCat.getOperationMap.get(HttpMethod.PUT)
      opCatPut.getOperationId must beEqualTo("add1")
      opCatPut.getParameters.head.getName must beEqualTo("cat")
      opCatPut.getParameters.head.getIn must beEqualTo("body")
      opCatPut.getParameters.head.asInstanceOf[BodyParameter].getSchema.getReference must beEqualTo("#/definitions/Cat")
      opCatPut.getConsumes must beNull
      opCatPut.getResponses.get("200").getSchema.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("ActionAnyContent")
      opCatPut.getProduces must beNull

      val pathDog = swagger.get.getPaths().get("/dog")
      pathDog.getOperations.size must beEqualTo(2)

      val opDogGet = pathDog.getOperationMap.get(HttpMethod.GET)
      opDogGet.getOperationId must beEqualTo("listDogs")
      opDogGet.getParameters must beEmpty
      opDogGet.getConsumes.asScala.toList must beEqualTo(List("application/json","application/xml"))
      opDogGet.getResponses.get("200").getSchema.asInstanceOf[ArrayProperty].getItems.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("Dog")
      opDogGet.getProduces.asScala.toList must beEqualTo(List("application/json","application/xml"))

      val opDogPut = pathDog.getOperationMap.get(HttpMethod.PUT)
      opDogPut.getOperationId must beEqualTo("add1")
      opDogPut.getParameters.head.getName must beEqualTo("dog")
      opDogPut.getParameters.head.getIn must beEqualTo("body")
      opDogPut.getParameters.head.asInstanceOf[BodyParameter].getSchema.getReference must beEqualTo("#/definitions/Dog")
      opDogPut.getConsumes.asScala.toList must beEqualTo(List("application/json","application/xml"))
      opDogPut.getResponses.get("200").getSchema.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("ActionAnyContent")
      opDogPut.getProduces.asScala.toList must beEqualTo(List("application/json","application/xml"))

      val pathDogParam = swagger.get.getPaths().get("/dog/{id}")
      pathDogParam.getOperations.size must beEqualTo(1)

      val opDogParamPut = pathDogParam.getOperationMap.get(HttpMethod.PUT)
      opDogParamPut.getOperationId must beEqualTo("add0")
      opDogParamPut.getParameters.head.getName must beEqualTo("id")
      opDogParamPut.getParameters.head.getIn must beEqualTo("path")
      opDogParamPut.getParameters.head.asInstanceOf[PathParameter].getType must beEqualTo("string")
      opDogParamPut.getConsumes.asScala.toList must beEqualTo(List("application/json","application/xml"))
      opDogParamPut.getProduces.asScala.toList must beEqualTo(List("application/json","application/xml"))
      opDogParamPut.getResponses.get("200").getSchema.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("ActionAnyContent")


      val catDef = swagger.get.getDefinitions.get("Cat").asInstanceOf[ModelImpl]
      catDef.getType must beEqualTo("object")
      catDef.getProperties.containsKey("id") must beTrue
      catDef.getProperties.containsKey("name") must beTrue

      val dogDef = swagger.get.getDefinitions.get("Dog").asInstanceOf[ModelImpl]
      dogDef.getType must beEqualTo("object")
      dogDef.getProperties.containsKey("id") must beTrue
      dogDef.getProperties.containsKey("name") must beTrue
    }
  }
  def toJsonString(data: Any): String = {
    if (data.getClass.equals(classOf[String])) {
      data.asInstanceOf[String]
    } else {
      Json.pretty(data.asInstanceOf[AnyRef])
    }
  }
}

