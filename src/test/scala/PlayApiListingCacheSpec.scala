import java.io.File

import io.swagger.config.ScannerFactory
import io.swagger.models.{HttpMethod, ModelImpl}
import io.swagger.models.parameters.{BodyParameter, PathParameter, QueryParameter}
import io.swagger.models.properties.{ArrayProperty, RefProperty}
import play.modules.swagger._
import org.specs2.mutable._
import org.specs2.mock.Mockito
import play.api.Logger
import play.api.Environment
import io.swagger.util.Json
import org.specs2.specification.BeforeAfterAll
import play.routes.compiler.Route

import scala.jdk.CollectionConverters._
import play.routes.compiler.{Route => PlayRoute}

class PlayApiListingCacheSpec extends Specification with Mockito with BeforeAfterAll {

  // set up mock for Play Router
  val routesList = {
    play.routes.compiler.RoutesFileParser.parseContent("""
POST /api/document/:settlementId/files/:fileId/accept testdata.DocumentController.accept(settlementId:String,fileId:String)
GET /api/searchapi testdata.SettlementsSearcherController.search(personalNumber:String,propertyId:String)
GET /api/pointsofinterest testdata.PointOfInterestController.list(eastingMin:Double,northingMin:Double,eastingMax:Double,northingMax:Double)
GET /api/dog testdata.DogController.list
GET /api/cat @testdata.CatController.list
GET /api/api/cat43 @testdata.CatController.testIssue43(test_issue_43_param: Option[Int])
PUT /api/cat @testdata.CatController.add1
GET /api/fly testdata.FlyController.list
PUT /api/dog testdata.DogController.add1
PUT /api/dog/api/:id testdata.DogController.add0(id:String)
    """, new File("")).right.get.collect {
      case route: PlayRoute => route
    }
  }

  val routesRules: Map[String, Route] = Map(routesList.map { route: Route =>
    (route.call.packageName.toSeq ++ Seq(route.call.controller + "$", route.call.method)).mkString(".") -> route
  }: _*)

  val apiVersion = "test1"
  val basePath = "/api"

  var swaggerConfig = PlaySwaggerConfig(
    description = "description",
    basePath = basePath,
    contact = "contact",
    host = "127.0.0.1",
    version = "beta",
    title = "title",
    termsOfServiceUrl = "http://termsOfServiceUrl",
    license = "license",
    licenseUrl = "http://licenseUrl",
    filterClass = None,
    schemes = Seq.empty
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

  "ApiListingCache" should {

    "load all API specs" in {

      val swagger = apiListingCache.listing("127.0.0.1")

      Logger("swagger").debug("swagger: " + toJsonString(swagger))

      swagger.getSwagger must beEqualTo("2.0")
      swagger.getBasePath must beEqualTo(basePath)
      swagger.getPaths.size must beEqualTo(7)
      swagger.getDefinitions.size must beEqualTo(10)
      swagger.getHost must beEqualTo(swaggerConfig.host)
      swagger.getInfo.getContact.getName must beEqualTo(swaggerConfig.contact)
      swagger.getInfo.getVersion must beEqualTo(swaggerConfig.version)
      swagger.getInfo.getTitle must beEqualTo(swaggerConfig.title)
      swagger.getInfo.getTermsOfService must beEqualTo(swaggerConfig.termsOfServiceUrl)
      swagger.getInfo.getLicense.getName must beEqualTo(swaggerConfig.license)
      swagger.getSecurityDefinitions.size() must beEqualTo(1)

      val pathDoc = swagger.getPaths.get("/document/{settlementId}/files/{fileId}/accept")
      pathDoc.getOperations.size must beEqualTo(1)

      val opDocPost = pathDoc.getOperationMap.get(HttpMethod.POST)
      opDocPost.getOperationId must beEqualTo("acceptsettlementfile")
      opDocPost.getParameters.size() must beEqualTo(3)
      opDocPost.getParameters.get(0).getDescription must beEqualTo("Id of the settlement to accept a file on.")
      opDocPost.getParameters.get(1).getDescription must beEqualTo("File id of the file to accept.")

      val pathSearch = swagger.getPaths.get("/searchapi")
      pathSearch.getOperations.size must beEqualTo(1)

      val opSearchGet = pathSearch.getOperationMap.get(HttpMethod.GET)
      opSearchGet.getParameters.size() must beEqualTo(3)
      opSearchGet.getDescription must beEqualTo("Search for a settlement with personal number and property id.")
      opSearchGet.getParameters.get(0).getDescription must beEqualTo("A personal number of one of the sellers.")
      opSearchGet.getParameters.get(1).getDescription must beEqualTo("The cadastre or share id.")

      val pathPOI = swagger.getPaths.get("/pointsofinterest")
      pathPOI.getOperations.size must beEqualTo(1)

      val opPOIGet = pathPOI.getOperationMap.get(HttpMethod.GET)
      opPOIGet.getParameters.size() must beEqualTo(5)
      opPOIGet.getDescription must beEqualTo("Returns points of interest")
      opPOIGet.getParameters.get(0).getDescription must beEqualTo("Minimum easting for provided extent")
      opPOIGet.getParameters.get(1).getDescription must beEqualTo("Minimum northing for provided extent")
      opPOIGet.getParameters.get(2).getDescription must beEqualTo("Maximum easting for provided extent")
      opPOIGet.getParameters.get(3).getDescription must beEqualTo("Maximum northing for provided extent")

      val pathCat = swagger.getPaths.get("/cat")
      pathCat.getOperations.size must beEqualTo(2)

      val opCatGet = pathCat.getOperationMap.get(HttpMethod.GET)
      opCatGet.getOperationId must beEqualTo("listCats")
      opCatGet.getParameters.asScala must beEmpty
      opCatGet.getConsumes must beNull
      opCatGet.getResponses.get("200").getSchema.asInstanceOf[ArrayProperty].getItems.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("Cat")
      opCatGet.getProduces must beNull

      val opCatPut = pathCat.getOperationMap.get(HttpMethod.PUT)
      opCatPut.getOperationId must beEqualTo("add1")
      opCatPut.getParameters.asScala.head.getName must beEqualTo("cat")
      opCatPut.getParameters.asScala.head.getIn must beEqualTo("body")
      opCatPut.getParameters.asScala.head.asInstanceOf[BodyParameter].getSchema.getReference must beEqualTo("#/definitions/Cat")
      opCatPut.getConsumes must beNull
      opCatPut.getResponses.get("200").getSchema.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("ActionAnyContent")
      opCatPut.getProduces must beNull
      opCatPut.getSecurity.get(0).get("oauth2").get(0) must beEqualTo("write_pets")

      val pathCat43 = swagger.getPaths.get("/api/cat43")
      pathCat43.getOperations.size must beEqualTo(1)

      val opCatGet43 = pathCat43.getOperationMap.get(HttpMethod.GET)
      opCatGet43.getOperationId must beEqualTo("test issue #43_nick")
      opCatGet43.getResponses.get("200").getSchema.asInstanceOf[ArrayProperty].getItems.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("Cat")

      opCatGet43.getParameters.asScala.head.getName must beEqualTo("test_issue_43_param")
      opCatGet43.getParameters.asScala.head.getIn must beEqualTo("query")
      opCatGet43.getParameters.asScala.head.asInstanceOf[QueryParameter].getType must beEqualTo("integer")

      opCatGet43.getParameters.get(1).getName must beEqualTo("test_issue_43_implicit_param")
      opCatGet43.getParameters.get(1).getIn must beEqualTo("query")
      opCatGet43.getParameters.get(1).asInstanceOf[QueryParameter].getType must beEqualTo("integer")

      val pathDog = swagger.getPaths.get("/dog")
      pathDog.getOperations.size must beEqualTo(2)

      val opDogGet = pathDog.getOperationMap.get(HttpMethod.GET)
      opDogGet.getOperationId must beEqualTo("listDogs")
      opDogGet.getParameters.asScala must beEmpty
      opDogGet.getConsumes.asScala.toList must beEqualTo(List("application/json","application/xml"))
      opDogGet.getResponses.get("200").getSchema.asInstanceOf[ArrayProperty].getItems.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("Dog")
      opDogGet.getProduces.asScala.toList must beEqualTo(List("application/json","application/xml"))

      val opDogPut = pathDog.getOperationMap.get(HttpMethod.PUT)
      opDogPut.getOperationId must beEqualTo("add1")
      opDogPut.getParameters.asScala.head.getName must beEqualTo("dog")
      opDogPut.getParameters.asScala.head.getIn must beEqualTo("body")
      opDogPut.getParameters.asScala.head.asInstanceOf[BodyParameter].getSchema.getReference must beEqualTo("#/definitions/Dog")
      opDogPut.getConsumes.asScala.toList must beEqualTo(List("application/json","application/xml"))
      opDogPut.getResponses.get("200").getSchema.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("ActionAnyContent")
      opDogPut.getProduces.asScala.toList must beEqualTo(List("application/json","application/xml"))

      val pathDogParam = swagger.getPaths.get("/dog/api/{id}")
      pathDogParam.getOperations.size must beEqualTo(1)

      val opDogParamPut = pathDogParam.getOperationMap.get(HttpMethod.PUT)
      opDogParamPut.getOperationId must beEqualTo("add0")
      opDogParamPut.getParameters.asScala.head.getName must beEqualTo("id")
      opDogParamPut.getParameters.asScala.head.getIn must beEqualTo("path")
      opDogParamPut.getParameters.asScala.head.asInstanceOf[PathParameter].getType must beEqualTo("string")
      opDogParamPut.getConsumes.asScala.toList must beEqualTo(List("application/json","application/xml"))
      opDogParamPut.getProduces.asScala.toList must beEqualTo(List("application/json","application/xml"))
      opDogParamPut.getResponses.get("200").getSchema.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("ActionAnyContent")

      val catDef = swagger.getDefinitions.get("Cat").asInstanceOf[ModelImpl]
      catDef.getType must beEqualTo("object")
      catDef.getProperties.containsKey("id") must beTrue
      catDef.getProperties.containsKey("name") must beTrue

      val dogDef = swagger.getDefinitions.get("Dog").asInstanceOf[ModelImpl]
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
