package io.aurora.utils.play.swagger

import io.aurora.utils.play.swagger.PlaySwaggerConfig._
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.{
  Contact => SwaggerContact,
  Info => SwaggerInfo,
  License => SwaggerLicense
}
import io.swagger.v3.oas.models.servers.Server
import org.apache.commons.lang3.StringUtils
import pureconfig.generic.ProductHint
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}

case class PlaySwaggerConfig(info: Info, host: String, basePath: String, schemes: Seq[String]) {

  def applyTo(swagger: OpenAPI): OpenAPI = {
    swagger.setInfo(info)

    val path = basePath
    val hostPath = if (StringUtils.isNotBlank(path)) s"$host/$path/" else host
    schemes.foreach { scheme =>
      swagger.addServersItem(new Server().url(s"${scheme.toLowerCase}://$hostPath"))
    }
    if (schemes.isEmpty && StringUtils.isNotBlank(hostPath)) swagger
      .addServersItem(new Server().url(hostPath))

    swagger
  }

}

object PlaySwaggerConfig {
  import pureconfig.generic.auto._

  case class Contact(name: Option[String], url: Option[String], email: Option[String])

  case class License(name: Option[String], url: Option[String])

  case class Info(
    title: Option[String],
    description: Option[String],
    version: Option[String],
    termsOfService: Option[String],
    contact: Option[Contact] = None,
    license: Option[License] = None
  )

  implicit
  def hint[AppConfig]: ProductHint[AppConfig] =
    ProductHint[AppConfig](ConfigFieldMapping(CamelCase, CamelCase))

  lazy val getConfig: PlaySwaggerConfig = ConfigSource.default.at("swagger.api")
    .loadOrThrow[PlaySwaggerConfig]

  implicit def scala2swagger(convertMe: Contact): SwaggerContact =
    if (convertMe.email.isEmpty) null
    else new SwaggerContact().name(convertMe.name.orNull).url(convertMe.url.orNull)
      .email(convertMe.email.orNull)

  implicit def scala2swagger(convertMe: License): SwaggerLicense =
    if (convertMe.name.isEmpty) null
    else new SwaggerLicense().name(convertMe.name.orNull).url(convertMe.url.orNull)

  implicit def scala2swagger(convertMe: Info): SwaggerInfo = new SwaggerInfo()
    .description(convertMe.description.orNull).version(convertMe.version.orNull)
    .title(convertMe.title.orNull).termsOfService(convertMe.termsOfService.orNull)
    .contact(convertMe.contact.getOrElse(null)).license(convertMe.license.getOrElse(null))

}
