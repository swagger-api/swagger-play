package play.modules.swagger

import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.core.SwaggerContext
import com.wordnik.swagger.config._

import play.api.Logger
import play.api.routing.Router
import play.modules.swagger.routes.Route
import play.modules.swagger.routes.{Route=>PlayRoute,Parameter => PlayParameter}
/**
 * Identifies Play Controllers annotated as Swagger API's.
 * Uses the Play Router to identify Controllers, and then tests each for the API annotation.
 */
class PlayApiScanner(routes: List[(String,PlayRoute)] ) extends Scanner {

  def classes(): List[Class[_]] = {
    Logger("swagger").info("ControllerScanner - looking for controllers with API annotation")

    // get controller names from application routes
    val controllers =         routes.map{ case (_,route) =>
          s"${route.call.packageName}.${route.call.controller}"
        }.distinct

    controllers.collect {
      case className: String if {
        try {
          SwaggerContext.loadClass(className).getAnnotation(classOf[Api]) != null
        } catch {
          case ex: Exception => {
            Logger("swagger").error("Problem loading class:  %s. %s: %s".format(className, ex.getClass.getName, ex.getMessage))
            false}
        }
      } =>
        Logger("swagger").info("Found API controller:  %s".format(className))
        SwaggerContext.loadClass(className)
    }

  }
}
