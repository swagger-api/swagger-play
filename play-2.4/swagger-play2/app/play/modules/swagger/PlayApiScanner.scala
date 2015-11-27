package play.modules.swagger

import io.swagger.annotations.Api
import io.swagger.config._
import io.swagger.models.{Contact, Info, License, Scheme, Swagger}
import org.apache.commons.lang3.StringUtils
import play.api.Logger
import play.modules.swagger.util.SwaggerContext

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
  * Identifies Play Controllers annotated as Swagger API's.
  * Uses the Play Router to identify Controllers, and then tests each for the API annotation.
  */
class PlayApiScanner() extends Scanner with SwaggerConfig {


  private def updateInfoFromConfig(swagger: Swagger): Swagger = {

    var info = new Info()
    val playSwaggerConfig = PlayConfigFactory.getConfig

    if (StringUtils.isNotBlank(playSwaggerConfig.description)) {
      info.description(playSwaggerConfig.description);
    }

    if (StringUtils.isNotBlank(playSwaggerConfig.title)) {
      info.title(playSwaggerConfig.title);
    } else {
      // title tag needs to be present to validate against schema
      info.title("");
    }

    if (StringUtils.isNotBlank(playSwaggerConfig.version)) {
      info.version(playSwaggerConfig.version);
    }

    if (StringUtils.isNotBlank(playSwaggerConfig.termsOfServiceUrl)) {
      info.termsOfService(playSwaggerConfig.termsOfServiceUrl);
    }

    if (playSwaggerConfig.contact != null) {
      info.contact(new Contact()
        .name(playSwaggerConfig.contact));
    }
    if (playSwaggerConfig.license != null && playSwaggerConfig.licenseUrl != null) {
      info.license(new License()
        .name(playSwaggerConfig.license)
        .url(playSwaggerConfig.licenseUrl));
    }
    swagger.info(info)
  }

  override def configure(swagger: Swagger): Swagger = {
    val playSwaggerConfig = PlayConfigFactory.getConfig
    if (playSwaggerConfig.schemes != null) {
      for (s <- playSwaggerConfig.schemes) swagger.scheme(Scheme.forValue(s))
    }
    updateInfoFromConfig(swagger)
    swagger.host(playSwaggerConfig.host)
    swagger.basePath(playSwaggerConfig.basePath);

  }

  override def getFilterClass(): String = {
    null
  }

  override def classes(): java.util.Set[Class[_]] = {
    Logger("swagger").info("ControllerScanner - looking for controllers with API annotation")


    var routes = RouteFactory.getRoute().getAll().toList

    // get controller names from application routes
    val controllers = routes.map { case (_, route) =>
      s"${route.call.packageName}.${route.call.controller}"
    }.distinct


    var list = controllers.collect {
      case className: String if {
        try {
          SwaggerContext.loadClass(className).getAnnotation(classOf[Api]) != null
        } catch {
          case ex: Exception => {
            Logger("swagger").error("Problem loading class:  %s. %s: %s".format(className, ex.getClass.getName, ex.getMessage))
            false
          }
        }
      } =>
        Logger("swagger").info("Found API controller:  %s".format(className))
        SwaggerContext.loadClass(className)
    }

    list.toSet.asJava

  }

  override def getPrettyPrint(): Boolean = {
    true;
  }

  override def setPrettyPrint(x: Boolean) {}
}
