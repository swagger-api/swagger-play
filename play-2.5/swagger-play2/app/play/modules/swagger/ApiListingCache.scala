package play.modules.swagger

import com.rits.cloning.Cloner
import io.swagger.config._
import io.swagger.models.Swagger
import play.api.Logger

object ApiListingCache {
  var cache: Option[Swagger] = None

  def listing(docRoot: String, host: String): Option[Swagger] = {
    val cloner = new Cloner();

    cache.orElse {
      Logger("swagger").debug("Loading API metadata")

      val scanner = ScannerFactory.getScanner()
      val classes = scanner.classes()
      val reader = new PlayReader(null)
      var swagger = reader.read(classes)

      scanner match {
        case config: SwaggerConfig => {
          swagger = config.configure(swagger)
        }
        case config => {
          // no config, do nothing
        }
      }
      cache = Some(swagger)
      val clone = cloner.deepClone(cache)
      clone
    }
    cache.get.setHost(host)
    val clone = cloner.deepClone(cache)
    clone
  }
}
