package play.modules.swagger

import io.swagger.annotations.Api
import io.swagger.config._
import io.swagger.models.Contact
import io.swagger.models.Info
import io.swagger.models.License
import io.swagger.models.Scheme
import io.swagger.models.Swagger
import javax.inject.Inject
import org.apache.commons.lang3.StringUtils
import play.api.Environment
import play.api.Logger

import scala.jdk.CollectionConverters._

/**
  * Identifies Play Controllers annotated as Swagger API's.
  * Uses the Play Router to identify Controllers, and then tests each for the API annotation.
  */
class PlayApiScanner @Inject()(config: PlaySwaggerConfig, routes: RouteWrapper, environment: Environment)
  extends Scanner with SwaggerConfig {
  private def updateInfoFromConfig(swagger: Swagger): Swagger = {

    val info = new Info()

    if (StringUtils.isNotBlank(config.description)) {
      info.description(config.description)
    }

    if (StringUtils.isNotBlank(config.title)) {
      info.title(config.title)
    } else {
      // title tag needs to be present to validate against schema
      info.title("")
    }

    if (StringUtils.isNotBlank(config.version)) {
      info.version(config.version)
    }

    if (StringUtils.isNotBlank(config.termsOfServiceUrl)) {
      info.termsOfService(config.termsOfServiceUrl)
    }

    if (config.contact != null) {
      info.contact(new Contact()
        .name(config.contact))
    }
    if (config.license != null && config.licenseUrl != null) {
      info.license(new License()
        .name(config.license)
        .url(config.licenseUrl))
    }
    swagger.info(info)
  }

  override def configure(swagger: Swagger): Swagger = {
    for (s <- config.schemes) swagger.scheme(Scheme.forValue(s))
    updateInfoFromConfig(swagger)
    swagger.host(config.host)
    swagger.basePath(config.basePath)
  }

  override def getFilterClass(): String = {
    null
  }

  override def classes(): java.util.Set[Class[_]] = {
    Logger("swagger").info("ControllerScanner - looking for controllers with API annotation")


    // get controller names from application routes
    val controllers = routes.getAll.toSeq.map { case (_, route) =>
      (route.call.packageName.toSeq :+ route.call.controller).mkString(".")
    }.distinct


    val list = controllers.collect {
      case className: String if {
        try {
          environment.classLoader.loadClass(className).getAnnotation(classOf[Api]) != null
        } catch {
          case ex: Exception => {
            Logger("swagger").error("Problem loading class:  %s. %s: %s".format(className, ex.getClass.getName, ex.getMessage))
            false
          }
        }
      } =>
        Logger("swagger").info("Found API controller:  %s".format(className))
        environment.classLoader.loadClass(className)
    }

    list.toSet.asJava

  }

  override def getPrettyPrint(): Boolean = true

  override def setPrettyPrint(x: Boolean): Unit = ()
}
