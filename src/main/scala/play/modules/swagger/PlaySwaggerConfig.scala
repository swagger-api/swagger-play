package play.modules.swagger

import com.typesafe.config.Config
import javax.annotation.Nullable
import play.api.Configuration

import scala.collection.JavaConverters._

case class Extension(name: String, value: AnyRef)

case class PlaySwaggerConfig(
  title: String,
  version: String,
  description: String,
  termsOfServiceUrl: String,
  contact: String,
  license: String,
  licenseUrl: String,
  host: String,
  basePath: String,
  schemes: Seq[String],
  filterClass: Option[String],
  vendorExtensions: List[Extension]
) {
  // Java APIs for reading the configuration
  def getSchemes: Array[String] = schemes.toArray
  @Nullable def getFilterClass: String = filterClass.orNull
}

object PlaySwaggerConfig {
  def defaultReference: PlaySwaggerConfig = PlaySwaggerConfig(Configuration.reference)

  def apply(configuration: Configuration): PlaySwaggerConfig = {
    PlaySwaggerConfig(
      version = configuration.get[String]("api.version"),
      description = configuration.get[String]("swagger.api.info.description"),
      host = configuration.get[String]("swagger.api.host"),
      basePath = configuration.get[String]("swagger.api.basepath"),
      schemes = configuration.get[Seq[String]]("swagger.api.schemes"),
      title = configuration.get[String]("swagger.api.info.title"),
      contact = configuration.get[String]("swagger.api.info.contact"),
      termsOfServiceUrl = configuration.get[String]("swagger.api.info.termsOfServiceUrl"),
      license = configuration.get[String]("swagger.api.info.license"),
      licenseUrl = configuration.get[String]("swagger.api.info.licenseUrl"),
      filterClass = configuration.get[Option[String]]("swagger.filter"),
      vendorExtensions = configuration.get[Option[Config]]("swagger.api.info").toList.map(_.entrySet()).flatMap { entries =>
        entries.asScala.filter(_.getKey.startsWith("x-")).map { entry =>
          Extension(entry.getKey, entry.getValue.unwrapped())
        }.toList.reverse
      }
    )
  }

  def apply(config: Config): PlaySwaggerConfig = apply(Configuration(config))
}
