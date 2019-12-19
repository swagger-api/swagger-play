package play.modules.swagger

import controllers.ApiHelpController
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class SwaggerModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =  Seq(
    bind[SwaggerPlugin].to[SwaggerPluginImpl].eagerly(),
    bind[ApiHelpController].toSelf.eagerly()
  )

}
