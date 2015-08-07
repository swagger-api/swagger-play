package play.modules.swagger

import scala.Predef._
import org.codehaus.jackson.map.ObjectMapper
import scala.collection.JavaConversions._
import play.{Play, Logger}
import collection.mutable.ListBuffer
import com.wordnik.swagger.core._
import javax.xml.bind.JAXBContext
import java.io.StringWriter

/**
  * Exposes two primay methods: to get a list of available resources and to get details on a given resource
  *
  * @author ayush
  * @since 10/9/11 4:05 PM
  *
  */
object ApiHelpInventory {
  // Add the Play classloader to Swagger
  SwaggerContext.registerClassLoader(Play.classloader)

  // Get a list of all controller classes
  private val controllerClasses = ListBuffer.empty[Class[_]]

  // Initialize the map from Api annotation value to play controller class
  private val resourceMap = scala.collection.mutable.Map.empty[String, Class[_]]

  // Read various configurable properties. These can be specified in application.conf
  private val apiVersion = Play.configuration.getProperty("api.version", "beta")
  private val basePath = Play.configuration.getProperty("swagger.api.basepath", "http://localhost")
  private val swaggerVersion = SwaggerSpec.version
  private val apiFilterClassName = Play.configuration.getProperty("swagger.security.filter")

  private val filterOutTopLevelApi = true

  def getResourceNames: java.util.List[String] = getResourceMap.keys.toList

  private val jaxbContext = JAXBContext.newInstance(classOf[Documentation]);
  private val jacksonObjectMapper = new ObjectMapper();

  /**
    * Get a list of all top level resources
    */
  private def getRootResources(format: String) = {
    var apiFilter: ApiAuthorizationFilter = null
    if(null != apiFilterClassName) {
      try {
        apiFilter = SwaggerContext.loadClass(apiFilterClassName).newInstance.asInstanceOf[ApiAuthorizationFilter]
      }
      catch {
        case e: ClassNotFoundException => Logger.error("Unable to resolve apiFilter class " + apiFilterClassName);
        case e: ClassCastException => Logger.error("Unable to cast to apiFilter class " + apiFilterClassName);
      }
    }

    val allApiDoc = new Documentation
    for (clazz <- getControllerClasses) {
      val apiAnnotation = clazz.getAnnotation(classOf[Api])
      if(null != apiAnnotation){
        val api = new DocumentationEndPoint(apiAnnotation.value + ".{format}", apiAnnotation.description())
        if(!isApiAdded(allApiDoc, api)) {
          if (null == apiFilter || apiFilter.authorizeResource(api.path, null, null)){
            allApiDoc.addApi(api)
          }
        }
      }
    }

    allApiDoc.swaggerVersion = swaggerVersion
    allApiDoc.basePath = basePath
    allApiDoc.apiVersion = apiVersion

    allApiDoc
  }

  /**
    * Get detailed API/models for a given resource
    */
  private def getResource(resourceName: String) = {
    getResourceMap.get(resourceName) match {
      case Some(clazz) => {
        val currentApiEndPoint = clazz.getAnnotation(classOf[Api])
        val currentApiPath = if (currentApiEndPoint != null && filterOutTopLevelApi) currentApiEndPoint.value else null

        val docs = new HelpApi(apiFilterClassName).filterDocs(
          PlayApiReader.read(clazz, apiVersion, swaggerVersion, basePath, currentApiPath), null, null, currentApiPath)

        Option(docs)
      }

      case None => None
    }

  }

  def getPathHelpJson(apiPath: String): String = {
    getResource(apiPath) match {
      case Some(docs) => jacksonObjectMapper.writeValueAsString(docs)
      case None => null
    }
  }

  def getPathHelpXml(apiPath: String): String = {
    getResource(apiPath) match {
      case Some(docs) => {
        val stringWriter = new StringWriter()
        jaxbContext.createMarshaller().marshal(docs, stringWriter);
        stringWriter.toString
      }
      case None => null
    }
  }

  def getRootHelpJson(apiPath: String): String = {
    jacksonObjectMapper.writeValueAsString(getRootResources("json"))
  }

  def getRootHelpXml(apiPath: String): String = {
      val stringWriter = new StringWriter()
      jaxbContext.createMarshaller().marshal(getRootResources("xml"), stringWriter);
      stringWriter.toString
  }


  /**
    * Get a list of all controller classes in Play
    */
  private def getControllerClasses = {
    if(this.controllerClasses.length == 0) {
      val classes = Play.classes.all().toList
      for (clazz <- classes) {
        if (clazz.name.startsWith("controllers.")) {
          if (clazz.javaClass != null && !clazz.javaClass.isInterface && !clazz.javaClass.isAnnotation) {
            controllerClasses += clazz.javaClass
          }
        }
      }

      if(controllerClasses.length > 0) {
        for (clazz <- controllerClasses) {
          val apiAnnotation = clazz.getAnnotation(classOf[Api])

          if (apiAnnotation != null) {
            Logger.debug("Identified Resource " + clazz.toString + " :: " + apiAnnotation.value)
            resourceMap += apiAnnotation.value -> clazz
          }
        }

      }
    }
   
    controllerClasses
  }

  private def getResourceMap = {
    // check if resources and controller info has already been loaded
    if(controllerClasses.length == 0) {
      this.getControllerClasses;
    }

    this.resourceMap;
  }

  private def isApiAdded(allApiDoc: Documentation, endpoint: DocumentationEndPoint): Boolean = {
    var isAdded: Boolean = false
    if (allApiDoc.getApis != null) {
      for (addedApi <- allApiDoc.getApis()) {
        if (endpoint.path.equals(addedApi.path)) isAdded = true
      }
    }
    isAdded
  }
}