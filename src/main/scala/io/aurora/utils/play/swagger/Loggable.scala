package io.aurora.utils.play.swagger

import play.api.Logger

trait Loggable {
  val logger: Logger = Logger("swagger")
}
