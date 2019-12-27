import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import play.modules.swagger.PlaySwaggerConfig

class PlayApiInfoConfigParserSpec extends Specification {

  private val config = ConfigFactory.load()

  "API Info " should {
    "proper build Info object" in {
      val probe = PlaySwaggerConfig(config)
      probe.title === "Test"
      probe.description === "Test API endpoint"
      probe.termsOfServiceUrl.isEmpty === true
      probe.contact === "test@test.com"
      probe.license ===  "Apache2"
      probe.licenseUrl ===  "http://licenseUrl"
      probe.vendorExtensions.size shouldEqual(2)
      probe.vendorExtensions.head.name === "x-api-key"
      probe.vendorExtensions.head.value === "test"
      probe.vendorExtensions.tail.head.name === "x-tags"
      probe.vendorExtensions.tail.head.value === java.util.Arrays.asList("test", "api")
    }
  }
}
