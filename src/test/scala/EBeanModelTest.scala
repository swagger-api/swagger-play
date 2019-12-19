import testdata._

import io.swagger.converter._

import org.specs2.mutable._
import org.specs2.mock.Mockito

class EBeanModelTest extends Specification with Mockito {
  "ModelConverters" should {
    "not parse an EBean" in {
      val models = ModelConverters.getInstance().readAll(classOf[Person])
      models.size must beEqualTo(1)

      val model = models.entrySet().iterator().next().getValue
      val property = model.getProperties.keySet().iterator().next()

      property must beEqualTo("name")
    }
  }
}
