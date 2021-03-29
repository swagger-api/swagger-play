package io.aurora.utils.play.swagger

import scala.collection.mutable

import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI

class ApiListingCache(
  scanner: PlayApiScanner,
  config: PlaySwaggerConfig
) extends Loggable {
  private val cache: mutable.Map[String, OpenAPI] = mutable.Map.empty

  def listing(host: String): OpenAPI = {
    cache.getOrElseUpdate(
      host, {
        logger.debug("Loading API metadata")

        val swagger = config.applyTo(new OpenAPI())
        val swaggerConfig = new SwaggerConfiguration()

        swaggerConfig.setReadAllResources(false)

        val reader = new PlayReader(
          swaggerConfig.openAPI(swagger),
          scanner.routes
        )

        reader.read(scanner.classes())
      }
    )
  }
}
