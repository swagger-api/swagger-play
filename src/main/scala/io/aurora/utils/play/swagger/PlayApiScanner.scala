package io.aurora.utils.play.swagger

import javax.inject.Inject

import scala.jdk.CollectionConverters._
import scala.util.Try

import io.swagger.v3.oas.annotations.Operation
import play.api.Environment

class PlayApiScanner @Inject() (val routes: RouteWrapper, environment: Environment)
    extends Loggable {

  def classes(): java.util.Set[Class[_]] = {
    logger.info("ControllerScanner - looking for controllers with API annotation")

    routes.getAll.toSeq.map { case (_, route) =>
      (route.call.packageName.toSeq :+ route.call.controller).mkString(".")
    }.distinct.collect {
      case className: String if hasOperationAnnotated(className) =>
        logger.info("Found API controller:  %s".format(className))
        environment.classLoader.loadClass(className)
    }.toSet.asJava
  }

  private def hasOperationAnnotated(className: String): Boolean = Try {
    environment.classLoader.loadClass(className).getMethods
      .exists(_.getAnnotation(classOf[Operation]) != null)
  }.toEither.fold(
    ex => {
      logger.error(
        "Problem loading class:  %s. %s: %s".format(className, ex.getClass.getName, ex.getMessage)
      )

      false
    },
    identity
  )

}
