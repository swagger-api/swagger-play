package play.modules.swagger
import java.net.URL
import javax.inject.{Inject, Provider}

import com.wordnik.swagger.config.{FilterFactory, ScannerFactory, ConfigFactory}
import com.wordnik.swagger.core.SwaggerContext
import com.wordnik.swagger.core.filter.SwaggerSpecFilter
import com.wordnik.swagger.model.ApiInfo
import com.wordnik.swagger.reader.ClassReaders
import play.api.inject.ApplicationLifecycle
import play.api.{Logger, Application}
import play.api.routing.Router
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class SwaggerPluginProvider extends Provider[SwaggerPlugin] {

  val logger = Logger("swagger")

  @Inject
  private var router: Router = _

  @Inject
  private var app: Application = _

  @Inject
  private var lifecycle: ApplicationLifecycle = _

  override def get(): SwaggerPlugin = {
    lifecycle.addStopHook(() => Future {
      onStop()
    })

    onStart()
  }

  def onStart(): SwaggerPlugin = {
    val config = app.configuration
    logger.info("Swagger - starting initialisation...")

    val apiVersion = config.getString("api.version") match {
      case None => "beta"
      case Some(value) => value
    }

    val basePath = config.getString("swagger.api.basepath")
      .filter(path => !path.isEmpty)
      .map(getPathUrl(_))
      .getOrElse("http://localhost:9000")

    val title = config.getString("swagger.api.info.title") match {
      case None => ""
      case Some(value)=> value
    }

    val description = config.getString("swagger.api.info.description") match {
      case None => ""
      case Some(value)=> value
    }

    val termsOfServiceUrl = config.getString("swagger.api.info.termsOfServiceUrl") match {
      case None => ""
      case Some(value)=> value
    }

    val contact = config.getString("swagger.api.info.contact") match {
      case None => ""
      case Some(value)=> value
    }

    val license = config.getString("swagger.api.info.license") match {
      case None => ""
      case Some(value)=> value
    }

    val licenseUrl = config.getString("swagger.api.info.licenseUrl") match {
      case None => ""
      case Some(value)=> value
    }

    ConfigFactory.config.setApiInfo(new ApiInfo(title, description, termsOfServiceUrl, contact, license, licenseUrl));

    SwaggerContext.registerClassLoader(app.classloader)
    ConfigFactory.config.setApiVersion(apiVersion)
    ConfigFactory.config.setBasePath(basePath)
    ScannerFactory.setScanner(new PlayApiScanner(Option(router)))
    ClassReaders.reader = Some(new PlayApiReader(Option(router)))

    app.configuration.getString("swagger.filter") match {
      case Some(e) if (e != "") => {
        try {
          FilterFactory.filter = SwaggerContext.loadClass(e).newInstance.asInstanceOf[SwaggerSpecFilter]
          Logger("swagger").info("Setting swagger.filter to %s".format(e))
        }
        catch {
          case ex: Exception => Logger("swagger").error("Failed to load filter " + e, ex)
        }
      }
      case _ =>
    }

    val docRoot = ""
    ApiListingCache.listing(docRoot)

    logger.info("Swagger - initialization done.")
    new SwaggerPlugin()
  }

  def onStop() {
    ApiListingCache.cache = None
    logger.info("Swagger - stopped.")
  }

  def loadFilter(filterClass: String): Unit = {
    try {
      FilterFactory.filter = SwaggerContext.loadClass(filterClass).newInstance.asInstanceOf[SwaggerSpecFilter]
      logger.info(s"Setting swagger.filter to $filterClass")
    } catch {
      case ex: Exception =>logger.error(s"Failed to load filter:$filterClass", ex)
    }
  }
  def getPathUrl(path: String): String = {
    try {
      val basePathUrl = new URL(path)
      logger.info(s"Basepath configured as:$path")
      path
    } catch {
      case ex: Exception =>
        logger.error(s"Misconfiguration - basepath not a valid URL:$path. Swagger abandoning initialisation!")
        throw ex
    }
  }

}