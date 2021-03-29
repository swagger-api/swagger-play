package io.aurora.utils.play.swagger.controllers

import javax.inject.Inject

import scala.util.control.NonFatal

import akka.util.ByteString
import io.aurora.utils.play.swagger.{Loggable, SwaggerPlugin}
import io.swagger.v3.core.util.{Json, Yaml}
import io.swagger.v3.oas.models.OpenAPI
import play.api.http.ContentTypes
import play.api.mvc._

class SwaggerController @Inject()(
  components: ControllerComponents,
  swaggerPlugin: SwaggerPlugin
) extends AbstractController(components) with Loggable {
  protected val AccessControlAllowOrigin: (String, String) = ("Access-Control-Allow-Origin", "*")

  def getResourcesAsJson: Action[AnyContent] = Action { _ =>
    val host: String = swaggerPlugin.config.host
    val response: String = generateSwaggerJson(swaggerPlugin.apiListingCache.listing(host))

    Results
      .Ok(ByteString(response))
      .as(ContentTypes.JSON)
      .withHeaders(AccessControlAllowOrigin)
  }

  def getResourcesAsYaml: Action[AnyContent] = Action { _ =>
    val host: String = swaggerPlugin.config.host
    val response: String = generateSwaggerYaml(swaggerPlugin.apiListingCache.listing(host))

    Results
      .Ok(ByteString(response))
      .as("text/vnd.yaml")
      .withHeaders(AccessControlAllowOrigin)
  }

  private def generateSwaggerYaml(openAPI: OpenAPI): String = {
    try {
      Yaml.pretty().writeValueAsString(openAPI)
    } catch {
      case NonFatal(t) =>
        logger.error("Issue with creating swagger.yaml", t)
        throw t
    }
  }

  private def generateSwaggerJson(openAPI: OpenAPI): String = {
    try {
      Json.pretty().writeValueAsString(openAPI)
    } catch {
      case NonFatal(t) =>
        logger.error("Issue with creating swagger.json", t)
        throw t
    }
  }
}
