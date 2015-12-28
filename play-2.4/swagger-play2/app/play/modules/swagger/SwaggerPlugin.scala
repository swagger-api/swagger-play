/**
 * Copyright 2014 Reverb Technologies, Inc.
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
import scala.collection.JavaConversions._
import play.routes.compiler.{Route => PlayRoute, Include => PlayInclude, RoutesFileParser, StaticPart}

trait SwaggerPlugin

class SwaggerPluginImpl @Inject()(lifecycle: ApplicationLifecycle, router: Router, app: Application) extends SwaggerPlugin {

  val logger = Logger("swagger")

  val config = app.configuration
  logger.info("Swagger - starting initialisation...")

  val apiVersion = config.getString("api.version") match {
    case None => "beta"
    case Some(value) => value
  }

  val basePath = config.getString("swagger.api.basepath")
    .filter(path => !path.isEmpty)
    .getOrElse("/")

  val host = config.getString("swagger.api.host")
    .filter(host => !host.isEmpty)
    .getOrElse("localhost:9000")

  val title = config.getString("swagger.api.info.title") match {
    case None => ""
    case Some(value)=> value
  }

  val description = config.getString("swagger.api.info.description") match {
    case None => ""
    case Some(value)=> value
  }

  val termsOfServiceUrl = config.getString("swagger.api.info.termsOfServiceUrl") match {
    case None => ""
    case Some(value)=> value
  }

  val contact = config.getString("swagger.api.info.contact") match {
    case None => ""
    case Some(value)=> value
  }

  val license = config.getString("swagger.api.info.license") match {
    case None => ""
    case Some(value)=> value
  }

  val licenseUrl = config.getString("swagger.api.info.licenseUrl") match {
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

    val routesFile = config.underlying.hasPath("play.http.router") match {
      case false => "routes"
      case true => config.getString("play.http.router") match {
        case None => "routes"
        case Some(value)=> playRoutesClassNameToFileName(value)
      }
    }
    //Parses multiple route files recursively
    def parseRoutesHelper(routesFile: String, prefix: String): List[PlayRoute] = {
      logger.debug(s"Processing route file '$routesFile' with prefix '$prefix'")
      val parsedRoutes = RoutesFileParser.parse(new File(app.classloader.getResource(routesFile).toURI))
      val routes = parsedRoutes.right.get.collect {
        case (route: PlayRoute) => {
          logger.debug(s"Adding route '$route'")
          Seq(route.copy(path = route.path.copy(parts = StaticPart(prefix + "/") +: route.path.parts)))
        }
        case (include: PlayInclude) => {
          logger.debug(s"Processing route include $include")
          parseRoutesHelper(playRoutesClassNameToFileName(include.router), include.prefix)
        }
      }.flatten
      logger.debug(s"Finished processing route file '$routesFile'")
      routes
    }
    parseRoutesHelper(routesFile, "")
  }

  val routesRules = Map(routes map
    { route =>
    {
      val routeName = s"${route.call.packageName}.${route.call.controller}$$.${route.call.method}"
      (routeName -> route)
    }
    } : _*)

  val route = new RouteWrapper(routesRules)
  RouteFactory.setRoute(route)
  app.configuration.getString("swagger.filter") match {
    case Some(e) if (e != "") => {
      try {
        FilterFactory setFilter SwaggerContext.loadClass(e).newInstance.asInstanceOf[SwaggerSpecFilter]
        logger.debug("Setting swagger.filter to %s".format(e))
      }
      catch {
        case ex: Exception => Logger("swagger").error("Failed to load filter " + e, ex)
      }
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
