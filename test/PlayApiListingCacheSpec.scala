import java.io.File

import io.swagger.config.ScannerFactory
import io.swagger.models.{ModelImpl, HttpMethod}
import io.swagger.models.parameters.{QueryParameter, BodyParameter, PathParameter}
import io.swagger.models.properties.{RefProperty, ArrayProperty}
import play.modules.swagger._
import org.specs2.mutable._
import org.specs2.mock.Mockito
import play.api.Logger
import io.swagger.util.Json
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import play.routes.compiler.{ Route => PlayRoute }

class PlayApiListingCacheSpec extends Specification with Mockito {

  // set up mock for Play Router
  val routesList = {
    play.routes.compiler.RoutesFileParser.parseContent("""
POST /api/document/:settlementId/files/:fileId/accept testdata.DocumentController.accept(settlementId:String,fileId:String)
GET /api/search testdata.SettlementsSearcherController.search(personalNumber:String,propertyId:String)
GET /api/pointsofinterest testdata.PointOfInterestController.list(eastingMin:Double,northingMin:Double,eastingMax:Double,northingMax:Double)
GET /api/dog testdata.DogController.list
PUT /api/dog testdata.DogController.add1
GET /api/cat @testdata.CatController.list
GET /api/cat43 @testdata.CatController.testIssue43(test_issue_43_param: Option[Int])
PUT /api/cat @testdata.CatController.add1
GET /api/fly testdata.FlyController.list
PUT /api/dog testdata.DogController.add1
PUT /api/dog/:id testdata.DogController.add0(id:String)
    """, new File("")).right.get.collect {
      case (route: PlayRoute) =>
        val routeName = s"${route.call.packageName}.${route.call.controller}$$.${route.call.method}"
        route
    }
  }

  val routesRules = Map(routesList map 
  { route =>
    {
      val routeName = s"${route.call.packageName}.${route.call.controller}$$.${route.call.method}"
      routeName -> route
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

      Logger.debug ("swagger: " + toJsonString(swagger))
      swagger must beSome

      swagger must beSome
      swagger.get.getSwagger must beEqualTo("2.0")
      swagger.get.getBasePath must beEqualTo(basePath)
      swagger.get.getPaths.size must beEqualTo(7)
      swagger.get.getDefinitions.size must beEqualTo(5)
      swagger.get.getHost must beEqualTo(swaggerConfig.getHost)
      swagger.get.getInfo.getContact.getName must beEqualTo(swaggerConfig.getContact)
      swagger.get.getInfo.getVersion must beEqualTo(swaggerConfig.getVersion)
      swagger.get.getInfo.getTitle must beEqualTo(swaggerConfig.getTitle)
      swagger.get.getInfo.getTermsOfService must beEqualTo(swaggerConfig.getTermsOfServiceUrl)
      swagger.get.getInfo.getLicense.getName must beEqualTo(swaggerConfig.getLicense)

      val pathDoc = swagger.get.getPaths.get("/document/{settlementId}/files/{fileId}/accept")
      pathDoc.getOperations.size must beEqualTo(1)

      val opDocPost = pathDoc.getOperationMap.get(HttpMethod.POST)
      opDocPost.getOperationId must beEqualTo("acceptsettlementfile")
      opDocPost.getParameters.size() must beEqualTo(3)
      opDocPost.getParameters.get(0).getDescription must beEqualTo("Id of the settlement to accept a file on.")
      opDocPost.getParameters.get(1).getDescription must beEqualTo("File id of the file to accept.")

      val pathSearch = swagger.get.getPaths.get("/search")
      pathSearch.getOperations.size must beEqualTo(1)

      val opSearchGet = pathSearch.getOperationMap.get(HttpMethod.GET)
      opSearchGet.getParameters.size() must beEqualTo(3)
      opSearchGet.getDescription must beEqualTo("Search for a settlement with personal number and property id.")
      opSearchGet.getParameters.get(0).getDescription must beEqualTo("A personal number of one of the sellers.")
      opSearchGet.getParameters.get(1).getDescription must beEqualTo("The cadastre or share id.")

      val pathPOI = swagger.get.getPaths.get("/pointsofinterest")
      pathPOI.getOperations.size must beEqualTo(1)

      val opPOIGet = pathPOI.getOperationMap.get(HttpMethod.GET)
      opPOIGet.getParameters.size() must beEqualTo(5)
      opPOIGet.getDescription must beEqualTo("Returns points of interest")
      opPOIGet.getParameters.get(0).getDescription must beEqualTo("Minimum easting for provided extent")
      opPOIGet.getParameters.get(1).getDescription must beEqualTo("Minimum northing for provided extent")
      opPOIGet.getParameters.get(2).getDescription must beEqualTo("Maximum easting for provided extent")
      opPOIGet.getParameters.get(3).getDescription must beEqualTo("Maximum northing for provided extent")

      val pathCat = swagger.get.getPaths.get("/cat")
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

      val pathCat43 = swagger.get.getPaths.get("/cat43")
      pathCat43.getOperations.size must beEqualTo(1)

      val opCatGet43 = pathCat43.getOperationMap.get(HttpMethod.GET)
      opCatGet43.getOperationId must beEqualTo("test issue #43_nick")
      opCatGet43.getResponses.get("200").getSchema.asInstanceOf[ArrayProperty].getItems.asInstanceOf[RefProperty].getSimpleRef must beEqualTo("Cat")

      opCatGet43.getParameters.head.getName must beEqualTo("test_issue_43_param")
      opCatGet43.getParameters.head.getIn must beEqualTo("query")
      opCatGet43.getParameters.head.asInstanceOf[QueryParameter].getType must beEqualTo("integer")

      opCatGet43.getParameters.get(1).getName must beEqualTo("test_issue_43_implicit_param")
      opCatGet43.getParameters.get(1).getIn must beEqualTo("query")
      opCatGet43.getParameters.get(1).asInstanceOf[QueryParameter].getType must beEqualTo("integer")



      val pathDog = swagger.get.getPaths.get("/dog")
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

      val pathDogParam = swagger.get.getPaths.get("/dog/{id}")
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

