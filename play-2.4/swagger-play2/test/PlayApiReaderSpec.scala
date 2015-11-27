import java.lang.reflect.Method

import play.api.Logger
import play.modules.swagger._

import org.specs2.mutable._
import org.specs2.mock.Mockito

import test.testdata.DogController

import play.modules.swagger.routes.{Route => PlayRoute, Parameter => PlayParameter, _}
import com.wordnik.swagger.core.SwaggerSpec
import com.wordnik.swagger.config.SwaggerConfig
import com.wordnik.swagger.model._

import org.mockito.Mockito._

class PlayApiReaderSpec extends Specification with Mockito {
  "PlayApiReader.SwaggerUtils" should {

    "convert a simple play route comment" in {
      val path = "/pet.json/$id<[^/]+>/test/$nothing<[^/]+>"
      SwaggerUtils.convertPathString(path) must be_==("/pet.json/{id}/test/{nothing}")
    }

    "convert a play route comment with numeric matchers" in {
      val path = """/pet.json/$id<[0-9]+>/test/$nothing<[^/]+>"""
      SwaggerUtils.convertPathString(path) must be_==("/pet.json/{id}/test/{nothing}")
    }
    "convert a play route comment with string matchers" in {
      val path = "/pet.json/$id<[A-Z]+>/test/$nothing<[^/]+>"
      SwaggerUtils.convertPathString(path) must be_==("/pet.json/{id}/test/{nothing}")
    }
    "convert a play route comment with complex matchers" in {
      val path = "/pet.json/$id<[A-Z0-9]+>/test/$nothing<[^/]+>"
      SwaggerUtils.convertPathString(path) must be_==("/pet.json/{id}/test/{nothing}")
    }
  }

  // set up mock for Play Router

  val routesRules =play.modules.swagger.routes.RoutesFileParser.parse("""
GET /api/dog test.testdata.DogController.list
GET /api/dog/:id test.testdata.DogController.get1(id: Long)
GET /api/dog/:dogId test.testdata.DogController.get2(dogId: Long)
PUT /api/dog test.testdata.DogController.add1
PUT /api/dog/:id test.testdata.DogController.add0(id:String)
POST /api/dog test.testdata.DogController.update
GET /api/cat @test.testdata.CatController.list
GET /api/cat/:id @test.testdata.CatController.get1(id: Long)
PUT /api/cat @test.testdata.CatController.add1
POST /api/cat @test.testdata.CatController.update
GET /api/fly test.testdata.FlyController.list
    """) match {
    case Right(rules) => rules.collect { case route: PlayRoute => route}
    case Left(error) => Logger.error(error.seq.toString())
      throw new RuntimeException("invalid route")

  }


  val basePath = "http://localhost/api"
  val swaggerConfig = new SwaggerConfig("1.2.3", SwaggerSpec.version, basePath, "")

  val reader = new PlayApiReader(routesRules)

  val dogControllerClass: Class[_] = DogController.getClass
  val catControllerClass: Class[_] = ClassLoader.getSystemClassLoader.loadClass("test.testdata.CatController")

  val get1Method = dogControllerClass.getMethod("get1", classOf[Long])
  val get2Method = dogControllerClass.getMethod("get2", classOf[Long])
  val get3Method = dogControllerClass.getMethod("get3", classOf[Long])
  val add0Method = dogControllerClass.getMethod("add0", classOf[String])

  def dogMethod(name: String) = {
    try
      Some(dogControllerClass.getMethod(name))
    catch {
      case ex: Exception => None
    }
  }

  def catMethod(name: String) = {
    try
      Some(catControllerClass.getMethod(name))
    catch {
      case ex: Exception => None
    }
  }

  def findRouteEntry(clazz:Class[_],method:String): RouteEntry = {
    new RouteEntry( routesRules.find{ r =>
      val controller = clazz.getSimpleName.replaceAll("\\$","")
      ( r.call.controller == controller && r.call.method == method ) }
      .getOrElse(new PlayRoute(HttpVerb(""), PathPattern(Seq.empty), HandlerCall("", "", true, "", None) )))
  }

  def findRouteEntry(clazz: Class[_], method: Method): RouteEntry = {
    findRouteEntry(clazz,method.getName)
  }


  def readDogMethod(name: String) = {
    val method = dogMethod(name).get
    val fullMethodName = reader.getFullMethodName(dogControllerClass, method)
    val routeEntry = findRouteEntry(dogControllerClass,name)

    reader.readMethod(method, fullMethodName,routeEntry)
  }

  "with Object as Controller" should {

    "get full name for method" in {
      reader.getFullMethodName(dogControllerClass, dogMethod("add1").get) must beEqualTo("test.testdata.DogController$.add1")
    }

    "get request path for a method" in {
      reader.getPath(dogControllerClass, dogMethod("add1").get).getOrElse("") must beEqualTo("/api/dog")
    }

    "get request path for a method that has no route defined" in {
      reader.getPath(dogControllerClass, dogMethod("no_route").get) must beEqualTo(None)
    }

    "get request path for a method that has path params" in {
      reader.getPath(dogControllerClass, get1Method).getOrElse("") must beEqualTo("/api/dog/{id}")
    }

  }

  "with Class as Controller" should {
    "get full name for method" in {
      reader.getFullMethodName(catControllerClass, catMethod("add1").get) must beEqualTo("test.testdata.CatController$.add1")
    }

    "get request path for a method" in {
      reader.getPath(catControllerClass, catMethod("add1").get).getOrElse("") must beEqualTo("/api/cat")
    }

    "get request path for a method that has no route defined" in {
      reader.getPath(catControllerClass, catMethod("no_route").get) must beEqualTo(None)
    }

    "get request path for a method that has path params" in {
      reader.getPath(catControllerClass, catControllerClass.getMethod("get1", classOf[Long])).getOrElse("") must beEqualTo("/api/cat/{id}")
    }

  }


  // todo add some more complex routes - multiple params etc..

  val mockOperation1 = mock[Operation]
  val mockOperation2 = mock[Operation]

  "appendOperation" should {

    "appendOperation should add operation to an empty Map" in {
      val operationsMap: Map[String, List[Operation]] = Map.empty
      val newOperationsMap = reader.appendOperation("key1", mockOperation1, operationsMap)

      newOperationsMap.get("key1").get.length must beEqualTo(1)
      newOperationsMap.get("key1").get.head must beEqualTo(mockOperation1)
    }

    "appendOperation should add operation to new key" in {
      val operationsMap: Map[String, List[Operation]] = Map(("key1", List(mockOperation1)))
      val newOperationsMap = reader.appendOperation("key2", mockOperation2, operationsMap)
      newOperationsMap.get("key1").get.length must beEqualTo(1)
      newOperationsMap.get("key2").get.length must beEqualTo(1)
      newOperationsMap.get("key1").get.head must beEqualTo(mockOperation1)
      newOperationsMap.get("key2").get.head must beEqualTo(mockOperation2)
    }

    "appendOperation should add operation to existing key" in {
      val operationsMap: Map[String, List[Operation]] = Map(("key1", List(mockOperation1)))
      val newOperationsMap = reader.appendOperation("key1", mockOperation2, operationsMap)
      newOperationsMap.get("key1").get.length must beEqualTo(2)
      newOperationsMap.get("key1").get.contains(mockOperation1) must beTrue
      newOperationsMap.get("key1").get.contains(mockOperation2) must beTrue
    }
  }



  "readMethod with Object as controller" should {

    "create Operation for annotated method" in {
      val maybeOperation: Option[Operation] = readDogMethod("list")
      maybeOperation.nonEmpty must beTrue
    }

    "does not creates Operation when no annotation" in {
      val maybeOperation: Option[Operation] = readDogMethod("no_annotations")
      "an operation should not have been returned" ! maybeOperation.isEmpty
    }

    "adds empty 'consumes' when not defined" in {
      val operation: Operation = readDogMethod("add1").get
      operation.consumes must beEqualTo(List.empty)
    }

    "adds 'consumes' when defined and trims" in {
      val operation: Operation = readDogMethod("add2").get
      operation.consumes.length must beEqualTo(1)
      operation.consumes.head must beEqualTo("application/json")
    }

    "adds multiple 'consumes' when defined and trims" in {
      val operation: Operation = readDogMethod("add3").get
      operation.consumes.length must beEqualTo(2)
      operation.consumes.contains("application/json") must beTrue
      operation.consumes.contains("text/yaml") must beTrue
    }

    "adds empty 'authorizations' when not defined" in {
      val operation: Operation = readDogMethod("add1").get
      operation.authorizations must beEqualTo(Nil)
    }

    "adds 'authorizations' when defined" in {
      val operation: Operation = readDogMethod("add2").get
      operation.authorizations.length must beEqualTo(1)
      operation.authorizations.head.`type` must beEqualTo("oauth2")
    }

    "adds multiple 'authorizations' when defined" in {
      val operation: Operation = readDogMethod("add3").get
      operation.authorizations.length must beEqualTo(2)
      operation.authorizations.map(_.`type`).contains("oauth2") must beTrue
    }

    "adds empty 'produces' when not defined" in {
      val fullMethodName = reader.getFullMethodName(dogControllerClass, get1Method)
      val routeEntry = findRouteEntry(dogControllerClass,get1Method)
      val operation: Operation = reader.readMethod(get1Method, fullMethodName,routeEntry).get
      operation.produces must beEqualTo(List.empty)
    }

    "adds 'produces' when defined and trims" in {
      val fullMethodName = reader.getFullMethodName(dogControllerClass, get2Method)
      val routeEntry = findRouteEntry(dogControllerClass,get2Method)
      val operation: Operation = reader.readMethod(get2Method, fullMethodName,routeEntry).get
      operation.produces.length must beEqualTo(1)
      operation.produces.head must beEqualTo("application/json")
    }

    "adds multiple 'produces' when defined and trims" in {
      val fullMethodName = reader.getFullMethodName(dogControllerClass, get3Method)
      val routeEntry = findRouteEntry(dogControllerClass,get3Method)
      val operation: Operation = reader.readMethod(get3Method, fullMethodName,routeEntry).get
      operation.produces.length must beEqualTo(2)
      operation.produces.contains("application/json") must beTrue
      operation.produces.contains("application/xml") must beTrue
    }

    "adds 'protocols' when defined and trims" in {
      val operation: Operation = readDogMethod("add2").get
      operation.protocols.length must beEqualTo(1)
      operation.protocols.head must beEqualTo("http")
    }

    "adds multiple 'protocols' when defined and trims" in {
      val operation: Operation = readDogMethod("add3").get
      operation.protocols.length must beEqualTo(2)
      operation.protocols.contains("http") must beTrue
      operation.protocols.contains("https") must beTrue
    }

    "adds empty 'protocols' when not defined" in {
      val operation: Operation = readDogMethod("add1").get
      operation.protocols must beEqualTo(List.empty)
    }

    "adds use route config" in {
      val fullMethodName = reader.getFullMethodName(dogControllerClass, add0Method)
      val routeEntry = findRouteEntry(dogControllerClass,add0Method)
      val operation: Operation = reader.readMethod(add0Method, fullMethodName,routeEntry).get

      operation.method must beEqualTo("PUT")
      operation.parameters.head must beEqualTo (Parameter("id", None, None, true, false, "java.lang.String", AnyAllowableValues, "path", None))
    }



    "adds 'deprecated' when defined" in {
      val operation: Operation = readDogMethod("deprecated").get
      operation.deprecated must beEqualTo(Some("true"))
    }

    "does not add 'deprecated' when not defined" in {
      val operation: Operation = readDogMethod("add1").get
      operation.deprecated must beEqualTo(None)
    }

    "adds 'method' when defined" in {
      val operation: Operation = readDogMethod("add1").get
      operation.method must beEqualTo("PUT")
    }

    "adds any-string for 'method' when defined" in {
      val operation: Operation = readDogMethod("unknown_method").get
      operation.method must beEqualTo("UNKNOWN")
    }

    "results in empty String for 'method' when not defined" in {
      val operation: Operation = readDogMethod("undefined_method").get
      operation.method must beEqualTo("")
    }

    "results in httpMethod + fullMethodName for 'nickname' when not defined" in {
      val operation: Operation = readDogMethod("add1").get
      val fullMethodName = reader.getFullMethodName(dogControllerClass, dogMethod("add1").get)
      operation.nickname.toLowerCase must beEqualTo(reader.genNickname(fullMethodName, "PUT").toLowerCase)
    }

    "adds 'nickname' when defined" in {
      val operation: Operation = readDogMethod("add2").get
      operation.nickname must beEqualTo("addDog2_nickname")
    }

    "results in empty String for 'notes' when not defined" in {
      val operation: Operation = readDogMethod("add1").get
      operation.notes must beEqualTo("")
    }

    "adds 'notes' when defined" in {
      val operation: Operation = readDogMethod("add2").get
      operation.notes must beEqualTo("Adds a dogs better")
    }


    "adds 'summary' when defined in 'value'" in {
      val operation: Operation = readDogMethod("add2").get
      operation.summary must beEqualTo("addDog2")
    }

    "defaults to 0 for 'position' when not defined" in {
      val operation: Operation = readDogMethod("add1").get
      operation.position must beEqualTo(0)
    }

    "adds 'position' when defined" in {
      val operation: Operation = readDogMethod("add2").get
      operation.position must beEqualTo(2)
    }

    "results in 0 'responseMessages' when not defined" in {
      val operation: Operation = readDogMethod("add1").get
      operation.responseMessages.length must beEqualTo(0)
    }

    "results in 0 'responseMessages' when defined as empty array" in {
      val operation: Operation = readDogMethod("add2").get
      operation.responseMessages.length must beEqualTo(0)
    }

    "adds multiple 'responseMessages' when defined" in {
      val operation: Operation = readDogMethod("add3").get
      operation.responseMessages.length must beEqualTo(2)
      operation.responseMessages.filter(responseMessage => responseMessage.code == 405).head.message must beEqualTo("Invalid input")
      operation.responseMessages.filter(responseMessage => responseMessage.code == 666).head.message must beEqualTo("Big Problem")
    }

    "returns 'void' for 'responseClass' when not defined" in {
      val operation: Operation = readDogMethod("add1").get
      operation.responseClass must beEqualTo("java.lang.Void")
    }

    "adds 'responseClass' when defined" in {
      val fullMethodName = reader.getFullMethodName(dogControllerClass, get1Method)
      val routeEntry = findRouteEntry(dogControllerClass,get1Method)
      val operation: Operation = reader.readMethod(get1Method, fullMethodName,routeEntry).get
      operation.responseClass must beEqualTo("test.testdata.Dog")
    }

    "adds 'responseClass' correctly when defined as list" in {
      val operation = readDogMethod("list").get
      operation.responseClass must beEqualTo("List[test.testdata.Dog]")
    }

    "results in 0 'parameters' when not defined" in {
      val operation: Operation = readDogMethod("list").get
      operation.parameters must beEqualTo(List.empty)
    }

    "adds 'parameters' when defined by @ApiParam" in {

      val fullMethodName = reader.getFullMethodName(dogControllerClass, get1Method)
      val routeEntry = findRouteEntry(dogControllerClass,get1Method)
      val operation: Operation = reader.readMethod(get1Method, fullMethodName,routeEntry).get
      operation.parameters.length must beEqualTo(1)
      operation.parameters.head must beEqualTo(Parameter("dogId", Some("ID of dog to fetch"), None, true, false, "long", AnyAllowableValues, "path", None))
    }

    "adds 'parameters' when defined by @ApiImplicitParams" in {
      val operation: Operation = readDogMethod("add1").get
      operation.parameters.length must beEqualTo(1)
      operation.parameters.head must beEqualTo(Parameter("dog", Some("Dog object to add"), None, true, false, "Dog", null, "body", None))
    }

  }

  "readMethod with Class as controller" should {

    "create Operation for annotated method" in {
      val listMethod = catMethod("list").get
      val fullMethodName = reader.getFullMethodName(catControllerClass, listMethod)
      val routeEntry = findRouteEntry(catControllerClass,listMethod)
      val maybeOperation: Option[Operation] = reader.readMethod(listMethod, fullMethodName,routeEntry)
      maybeOperation.nonEmpty must beTrue
    }
  }

  "read" should {

    "create ApiListing for annotated Controller Object" in {
      val maybeApiListing = reader.read("docroot", dogControllerClass, swaggerConfig)
      maybeApiListing.nonEmpty must beTrue
      val apiListing = maybeApiListing.get
      apiListing.apis must not be empty
      apiListing.apis.length must beEqualTo(3)
      apiListing.apis.map {
        api =>
          println("path: %s".format(api.path))
          println("description: %s".format(api.description))
          println("operations: %s".format(api.operations))
          println("======")
      }
      val api1 = apiListing.apis.find{api =>
        api.path == "/dog/{id}" }
      api1 must beSome
      api1.get.operations must not be empty
      api1.get.operations.length must beEqualTo(2)
    }

    "create ApiListing for annotated Controller Class" in {
      val maybeApiListing = reader.read("docroot", catControllerClass, swaggerConfig)
      maybeApiListing.nonEmpty must beTrue
      val apiListing = maybeApiListing.get
      apiListing.apis must not be empty
      apiListing.apis.length must beEqualTo(2)
      apiListing.apis.map {
        api =>
          println("path: %s".format(api.path))
          println("description: %s".format(api.description))
          println("operations: %s".format(api.operations))
          println("======")
      }
      val api1 = apiListing.apis.find(api => api.path == "/cat/{id}")
      api1 must beSome
      api1.get.operations must not be empty
      api1.get.operations.length must beEqualTo(1)
    }

    //todo -  more to add...
  }


}
