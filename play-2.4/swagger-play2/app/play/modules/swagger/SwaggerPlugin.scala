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

import java.net.URL
import javax.inject.Inject

import com.wordnik.swagger.config.{FilterFactory, ScannerFactory, ConfigFactory}
import com.wordnik.swagger.core.SwaggerContext
import com.wordnik.swagger.core.filter.SwaggerSpecFilter
import com.wordnik.swagger.model.ApiInfo
import com.wordnik.swagger.reader.ClassReaders
import play.api.inject.ApplicationLifecycle
import play.api.{Logger, Application}
import play.api.routing.Router
import play.modules.swagger.routes.Route
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import play.modules.swagger.routes.{Route=>PlayRoute,Parameter => PlayParameter}

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
    .map(getPathUrl(_))
    .getOrElse("http://localhost:9000")

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
    case None => ""
    case Some(value)=> value
  }

  ConfigFactory.config.setApiInfo(new ApiInfo(title, description, termsOfServiceUrl, contact, license, licenseUrl));

  SwaggerContext.registerClassLoader(app.classloader)
  ConfigFactory.config.setApiVersion(apiVersion)
  ConfigFactory.config.setBasePath(basePath)



  val routes ={
      play.modules.swagger.routes.RoutesFileParser.parse(app.classloader,"routes","").right.get.collect {
        case (prefix, route: PlayRoute) => (prefix,route)
      }
    }

  ClassReaders.reader = Some(new PlayApiReader(routes))

  ScannerFactory.setScanner(new PlayApiScanner(routes))


  app.configuration.getString("swagger.filter") match {
    case Some(e) if (e != "") => {
      try {
        FilterFactory.filter = SwaggerContext.loadClass(e).newInstance.asInstanceOf[SwaggerSpecFilter]
        Logger("swagger").info("Setting swagger.filter to %s".format(e))
      }
      catch {
        case ex: Exception => Logger("swagger").error("Failed to load filter " + e, ex)
      }
    }
    case _ =>
  }

  val docRoot = ""
  ApiListingCache.listing(docRoot)

  logger.info("Swagger - initialization done.")

  // previous contents of Plugin.onStart
  lifecycle.addStopHook { () =>
    ApiListingCache.cache = None
    logger.info("Swagger - stopped.")

    Future.successful(())
  }

  private def getPathUrl(path: String): String = {
    try {
      val basePathUrl = new URL(path)
      logger.info(s"Basepath configured as:$path")
      path
    } catch {
      case ex: Exception =>
        logger.error(s"Misconfiguration - basepath not a valid URL:$path. Swagger abandoning initialisation!")
        throw ex
    }
  }

}
