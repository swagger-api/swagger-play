/**
 * Copyright 2019 SmartBear Software, Inc.
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
import javax.inject.Inject
import io.swagger.config.{FilterFactory, ScannerFactory}
import play.modules.swagger.util.SwaggerContext
import io.swagger.core.filter.SwaggerSpecFilter
import play.api.inject.ApplicationLifecycle
import play.api.{Logger, Application}
import play.api.routing.Router
import scala.concurrent.Future
import scala.collection.JavaConverters._
import play.routes.compiler.{Route => PlayRoute, Include => PlayInclude, RoutesFileParser, StaticPart}

import scala.io.Source

trait SwaggerPlugin

class SwaggerPluginImpl @Inject()(lifecycle: ApplicationLifecycle, router: Router, app: Application) extends SwaggerPlugin {

  val logger = Logger("swagger")

  val config = app.configuration
  logger.info("Swagger - starting initialisation...")

  val apiVersion = config.getOptional[String]("api.version") match {
    case None => "beta"
    case Some(value) => value
  }

  val basePath = config.getOptional[String]("swagger.api.basepath")
    .filter(path => !path.isEmpty)
    .getOrElse("/")

  val host = config.getOptional[String]("swagger.api.host")
    .filter(host => !host.isEmpty)
    .getOrElse("localhost:9000")

  val title = config.getOptional[String]("swagger.api.info.title") match {
    case None => ""
    case Some(value)=> value
  }

  val description = config.getOptional[String]("swagger.api.info.description") match {
    case None => ""
    case Some(value)=> value
  }

  val termsOfServiceUrl = config.getOptional[String]("swagger.api.info.termsOfServiceUrl") match {
    case None => ""
    case Some(value)=> value
  }

  val contact = config.getOptional[String]("swagger.api.info.contact") match {
    case None => ""
    case Some(value)=> value
  }

  val license = config.getOptional[String]("swagger.api.info.license") match {
    case None => ""
    case Some(value)=> value
  }

  val licenseUrl = config.getOptional[String]("swagger.api.info.licenseUrl") match {
    // licenceUrl needs to be a valid URL to validate against schema
    case None => "http://licenseUrl"
    case Some(value)=> value
  }

  SwaggerContext.registerClassLoader(app.classloader)

  var scanner = new PlayApiScanner()
  ScannerFactory.setScanner(scanner)

  var swaggerConfig = new PlaySwaggerConfig()

  swaggerConfig.description = description
  swaggerConfig.basePath = basePath
  swaggerConfig.contact = contact
  swaggerConfig.version = apiVersion
  swaggerConfig.title = title
  swaggerConfig.host = host
  swaggerConfig.termsOfServiceUrl = termsOfServiceUrl
  swaggerConfig.license = license
  swaggerConfig.licenseUrl = licenseUrl

  PlayConfigFactory.setConfig(swaggerConfig)


  val routes = parseRoutes

  def parseRoutes: List[PlayRoute] = {
    def playRoutesClassNameToFileName(className: String) = className.replace(".Routes", ".routes")

    val routesFile = if (config.underlying.hasPath("play.http.router")) {
      config.getOptional[String]("play.http.router") match {
        case None => "routes"
        case Some(value) => playRoutesClassNameToFileName(value)
      }
    } else {
      "routes"
    }
    //Parses multiple route files recursively
    def parseRoutesHelper(routesFile: String, prefix: String): List[PlayRoute] = {
      logger.debug(s"Processing route file '$routesFile' with prefix '$prefix'")

      val routesContent =  Source.fromInputStream(app.classloader.getResourceAsStream(routesFile)).mkString
      val parsedRoutes = RoutesFileParser.parseContent(routesContent,new File(routesFile))
      val routes = parsedRoutes.right.get.collect {
        case route: PlayRoute =>
          logger.debug(s"Adding route '$route'")
          Seq(route.copy(path = route.path.copy(parts = StaticPart(prefix) +: route.path.parts)))
        case include: PlayInclude =>
          logger.debug(s"Processing route include $include")
          parseRoutesHelper(playRoutesClassNameToFileName(include.router), include.prefix)
      }.flatten
      logger.debug(s"Finished processing route file '$routesFile'")
      routes
    }
    parseRoutesHelper(routesFile, "")
  }

  val routesRules = routes.map(route => s"${route.call.packageName.map(_ + ".").getOrElse("")}${route.call.controller}$$.${route.call.method}" -> route).toMap

  val route = new RouteWrapper(routesRules.asJava)
  RouteFactory.setRoute(route)
  app.configuration.getOptional[String]("swagger.filter") match {
    case Some(e) if e != "" =>
      try {
        FilterFactory setFilter SwaggerContext.loadClass(e).newInstance.asInstanceOf[SwaggerSpecFilter]
        logger.debug("Setting swagger.filter to %s".format(e))
      }
      catch {
        case ex: Exception => Logger("swagger").error("Failed to load filter " + e, ex)
      }
    case _ =>
  }

  val docRoot = ""
  ApiListingCache.listing(docRoot, "127.0.0.1")

  logger.info("Swagger - initialization done.")

  // previous contents of Plugin.onStart
  lifecycle.addStopHook { () =>
    ApiListingCache.cache = None
    logger.info("Swagger - stopped.")

    Future.successful(())
  }

}
