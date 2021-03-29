package io.aurora.utils.play.swagger

import java.io.File
import javax.inject.Inject

import scala.io.Source

import io.swagger.v3.oas.models.OpenAPI
import play.api.{Configuration, Environment}
import play.routes.compiler.{RoutesFileParser, StaticPart, Include => PlayInclude, Route => PlayRoute}

trait SwaggerPlugin {
  def config: PlaySwaggerConfig
  def apiListingCache: ApiListingCache
  def scanner: PlayApiScanner
  def routes: RouteWrapper

  def getOpenApi: OpenAPI = apiListingCache.listing(config.host)
}

class SwaggerPluginImpl @Inject()(environment: Environment, configuration: Configuration) extends SwaggerPlugin {
  lazy val config: PlaySwaggerConfig = PlaySwaggerConfig.getConfig

  lazy val routes = new RouteWrapper({
    val routesFile = configuration.get[Option[String]]("play.http.router") match {
      case None => "routes"
      case Some(value) => SwaggerPluginHelper.playRoutesClassNameToFileName(value)
    }

    SwaggerPluginHelper
      .parseRoutes(routesFile, "", environment)
      .map { route =>
        val call = route.call
        val routeName = (call.packageName.toSeq ++ Seq(call.controller + "$", call.method)).mkString(".")
        routeName -> route
      }
      .toMap
  })

  lazy val scanner = new PlayApiScanner(routes, environment)
  lazy val apiListingCache = new ApiListingCache(scanner, config)
}

object SwaggerPluginHelper extends Loggable {
  def playRoutesClassNameToFileName(className: String): String = className.replace(".Routes", ".routes")

  def parseRoutes(routesFile: String, prefix: String, env: Environment): List[PlayRoute] = {
    logger.debug(s"Processing route file '$routesFile' with prefix '$prefix'")

    val parsedRoutes = env
      .resourceAsStream(routesFile)
      .map { stream =>
        val routesContent = Source.fromInputStream(stream).mkString
        RoutesFileParser.parseContent(routesContent, new File(routesFile))
      }
      .getOrElse(Right(List.empty))

    val routes = parsedRoutes
      .getOrElse(throw new NoSuchElementException("Parsed routes not found!"))
      .collect {
        case route: PlayRoute =>
          logger.debug(s"Adding route '$route'")

          (prefix, route.path.parts) match {
            case ("", _) => Seq(route)
            case (_, Seq()) => Seq(route.copy(path = route.path.copy(parts = StaticPart(prefix) +: route.path.parts)))
            case (_, Seq(StaticPart(""))) =>
              Seq(route.copy(path = route.path.copy(parts = StaticPart(prefix) +: route.path.parts)))
            case (_, Seq(StaticPart("/"))) =>
              Seq(route.copy(path = route.path.copy(parts = StaticPart(prefix) +: route.path.parts)))
            case (_, _) =>
              Seq(route.copy(path = route.path.copy(parts = StaticPart(prefix) +: StaticPart("/") +: route.path.parts)))
          }
        case include: PlayInclude =>
          logger.debug(s"Processing route include $include")

          val newPrefix = if (prefix == "") {
            include.prefix
          } else {
            s"$prefix/${include.prefix}"
          }

          parseRoutes(playRoutesClassNameToFileName(include.router), newPrefix, env)
      }.flatten

    logger.debug(s"Finished processing route file '$routesFile'")

    routes
  }
}
