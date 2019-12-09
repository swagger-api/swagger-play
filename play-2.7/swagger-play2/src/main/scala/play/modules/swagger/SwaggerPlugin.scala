/**
 * Copyright 2017 SmartBear Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package play.modules.swagger

import java.io.File

import io.swagger.config.FilterFactory
import io.swagger.config.Scanner
import io.swagger.config.ScannerFactory
import io.swagger.core.filter.SwaggerSpecFilter
import javax.inject.Inject
import play.api.Configuration
import play.api.Environment
import play.api.Logger
import play.routes.compiler.RoutesFileParser
import play.routes.compiler.StaticPart
import play.routes.compiler.{Include => PlayInclude}
import play.routes.compiler.{Route => PlayRoute}

import scala.io.Source

trait SwaggerPlugin {
  def config: PlaySwaggerConfig
  def apiListingCache: ApiListingCache
  def playReader: PlayReader
  def scanner: Scanner
  def routes: RouteWrapper
  def swaggerSpecFilter: Option[SwaggerSpecFilter]
}

class SwaggerPluginImpl @Inject()(environment: Environment, configuration: Configuration) extends SwaggerPlugin {

  private val logger = Logger("swagger")

  lazy val config: PlaySwaggerConfig = PlaySwaggerConfig(configuration)

  lazy val routes = new RouteWrapper({
    val routesFile = configuration.get[Option[String]]("play.http.router") match {
      case None => "routes"
      case Some(value) => SwaggerPluginHelper.playRoutesClassNameToFileName(value)
    }

    val routesList = SwaggerPluginHelper.parseRoutes(routesFile, "", environment)

    Map(routesList map { route =>
      val call = route.call
      val routeName = (call.packageName.toSeq ++ Seq(call.controller + "$", call.method)).mkString(".")
      routeName -> route
    }: _*)
  })

  lazy val scanner = new PlayApiScanner(config, routes, environment)

  lazy val swaggerSpecFilter: Option[SwaggerSpecFilter] = config.filterClass match {
    case Some(e) if e.nonEmpty =>
      try {
        val filter = environment.classLoader.loadClass(e).newInstance.asInstanceOf[SwaggerSpecFilter]
        logger.debug("Setting swagger.filter to %s".format(e))
        Some(filter)
      } catch {
        case ex: Exception =>
          Logger("swagger").error("Failed to load filter " + e, ex)
          None
      }
    case _ =>
      None
  }

  lazy val playReader = new PlayReader(config, routes, null)
  lazy val apiListingCache = new ApiListingCache(scanner, playReader)

  logger.info("Swagger: starting initialization")
  // eagerly initialize and set up global state required by Swagger
  swaggerSpecFilter foreach FilterFactory.setFilter
  ScannerFactory.setScanner(scanner)
  logger.info("Swagger: initialization done")
}

object SwaggerPluginHelper {
  private val logger = Logger("swagger")

  def playRoutesClassNameToFileName(className: String): String = className.replace(".Routes", ".routes")

  // Parses multiple route files recursively
  def parseRoutes(routesFile: String, prefix: String, env: Environment): List[PlayRoute] = {
    logger.debug(s"Processing route file '$routesFile' with prefix '$prefix'")

    val parsedRoutes = env.resourceAsStream(routesFile).map { stream =>
      val routesContent = Source.fromInputStream(stream).mkString
      RoutesFileParser.parseContent(routesContent, new File(routesFile))
    }.getOrElse(Right(List.empty)) // ignore routes files that don't exist

    val routes = parsedRoutes.getOrElse(throw new NoSuchElementException("Parsed routes not found!")).collect {
      case route: PlayRoute =>
        logger.debug(s"Adding route '$route'")
        (prefix, route.path.parts) match {
          case ("", _) => Seq(route)
          case (_, Seq()) => Seq(route.copy(path = route.path.copy(parts = StaticPart(prefix) +: route.path.parts)))
          case (_, Seq(StaticPart(""))) => Seq(route.copy(path = route.path.copy(parts = StaticPart(prefix) +: route.path.parts)))
          case (_, Seq(StaticPart("/"))) => Seq(route.copy(path = route.path.copy(parts = StaticPart(prefix) +: route.path.parts)))
          case (_, _) => Seq(route.copy(path = route.path.copy(parts = StaticPart(prefix) +: StaticPart("/") +: route.path.parts)))
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
