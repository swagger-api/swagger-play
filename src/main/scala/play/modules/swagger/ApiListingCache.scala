package play.modules.swagger

import io.swagger.config._
import io.swagger.models.Swagger
import javax.inject.Inject
import play.api.Logger
import scala.collection.mutable

class ApiListingCache @Inject()(scanner: Scanner, reader: PlayReader) {
  private val cache: mutable.Map[String, Swagger] = mutable.Map.empty

  def listing(host: String): Swagger = {
    cache.getOrElseUpdate(host, {
      Logger("swagger").debug("Loading API metadata")

      val classes = scanner.classes()
      val swagger = reader.read(classes)
      val result = scanner match {
        case config: SwaggerConfig =>
          config.configure(swagger)
        case _ =>
          swagger
      }
      result.setHost(host)
      result
    })
  }
}
