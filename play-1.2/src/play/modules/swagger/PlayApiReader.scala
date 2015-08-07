package play.modules.swagger

import java.lang.reflect.Method
import com.wordnik.swagger.core._
import collection.JavaConversions._
import play.mvc.Router
import play.Logger
import play.classloading.enhancers.LocalvariablesNamesEnhancer

/**
  * Caches and retrieves API information for a given Swagger compatible class
  *
  * @author ayush
  * @since 10/9/11 7:13 PM
  *
  */
object PlayApiReader {
  private val endpointsCache = scala.collection.mutable.Map.empty[Class[_], Documentation]

  def read(hostClass: Class[_], apiVersion: String, swaggerVersion: String, basePath: String, apiPath: String): Documentation = {
    endpointsCache.get(hostClass) match {
      case None => val doc = new PlayApiSpecParser(hostClass, apiVersion, swaggerVersion, basePath, apiPath).parse; endpointsCache += hostClass -> doc.clone.asInstanceOf[Documentation]; doc
      case doc: Option[Documentation] => doc.get.clone.asInstanceOf[Documentation]
      case _ => null
    }
  }
}

/**
  * Reads swaggers annotations, play route information and uses reflection to build API information on a given class
  */
private class PlayApiSpecParser(hostClass: Class[_], apiVersion: String, swaggerVersion: String, basePath: String, resourcePath: String)
  extends ApiSpecParser(hostClass, apiVersion, swaggerVersion, basePath, resourcePath) {

  // regular expression to extract content between {content}
  private val RX = """\{([^}]+)\}""".r
  private val POST = "post"

  override protected def processOperation(method: Method, o: DocumentationOperation) = {

    // set param names from method signatures
    // assuming natural order
    if (o.getParameters() != null && o.getParameters().length > 0) {
      val paramNames = LocalvariablesNamesEnhancer.lookupParameterNames(method).toList

      if (paramNames.length == o.getParameters().length) {
        var index = 0
        for (param <- o.getParameters()) {
          param.name = readString(paramNames.get(index), param.name)
          index = index + 1
        }
      }
    }


    // set http parameter type (query/path/body)
    getRoute(method) match {
      case Some(route) => {
        o.httpMethod = route.method

        if (o.getParameters() != null && o.getParameters().length > 0) {
          val routeHasPathParams = route.path.contains("{")

          if (routeHasPathParams) {
            val routePathParams: List[String] = (RX findAllIn route.path).toList

            for (param <- o.getParameters()) {
              param.paramType = routePathParams.find(_.equals("{" + param.name + "}")) match {
                case Some(p) => ApiValues.TYPE_PATH
                case None => if(POST.equalsIgnoreCase(route.method)) ApiValues.TYPE_BODY else ApiValues.TYPE_QUERY
              }
            }
          } else {
            for (param <- o.getParameters()) {
              param.paramType = if(POST.equalsIgnoreCase(route.method)) ApiValues.TYPE_BODY else ApiValues.TYPE_QUERY
            }
          }

        }
      }
      case None => Logger.info("Cannot process Operation. Nothing defined in play routes file for api method " + method.toString);
    }

    o
  }

  /**
    * Get the path which routes file points to for a given controller method
    */
  override protected def getPath(method: Method) = {
    getRoute(method) match {
      case Some(route) => route.path.replace(".json", ".{format}").replace(".xml", ".{format}")
      case None => Logger.info("Cannot determine Path. Nothing defined in play routes file for api method " + method.toString); this.resourcePath
    }
  }

  /**
    * Get the play route corresponding to a given java.lang.Method instance of a play controller method
    */
  private def getRoute(method: Method) = Router.routes.find((route) => {
    val parts = route.action.split("\\.")
    if (parts.length == 2) {
      val className = parts(0)
      val methodName = parts(1)

      val targetClassName = method.getDeclaringClass.getSimpleName
      val targetMethodName = method.getName

      // checking indexOf instead of equals to be compatible with scala where $ may be suffixed to controller class name
      targetClassName.indexOf(className) == 0 && targetMethodName.equals(methodName)
    } else {
      false
    }
  })
}
