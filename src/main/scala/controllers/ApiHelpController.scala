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

package controllers

import java.io.StringWriter

import akka.util.ByteString
import io.swagger.core.filter.SpecFilter
import io.swagger.models.Swagger
import io.swagger.util.Json
import javax.inject.Inject
import javax.xml.bind.annotation._
import play.api.Logger
import play.api.http.ContentTypes
import play.api.mvc._
import play.modules.swagger.SwaggerPlugin

import scala.jdk.CollectionConverters._

object ErrorResponse {
  val ERROR = 1
  val WARNING = 2
  val INFO = 3
  val OK = 4
  val TOO_BUSY = 5
}

class ErrorResponse(@XmlElement var code: Int, @XmlElement var message: String) {
  def this() = this(0, null)

  @XmlTransient
  def getCode: Int = code

  def setCode(code: Int) = this.code = code

  def getType: String = code match {
    case ErrorResponse.ERROR => "error"
    case ErrorResponse.WARNING => "warning"
    case ErrorResponse.INFO => "info"
    case ErrorResponse.OK => "ok"
    case ErrorResponse.TOO_BUSY => "too busy"
    case _ => "unknown"
  }

  def getMessage: String = message

  def setMessage(message: String) = this.message = message
}

class ApiHelpController @Inject() (components: ControllerComponents, val swaggerPlugin: SwaggerPlugin)
  extends AbstractController(components) with SwaggerBaseApiController {

  def getResources = Action { implicit request =>
    val host: String = swaggerPlugin.config.host
    val resourceListing: Swagger = getResourceListing(host)
    val response: String = returnXml(request) match {
      case true => toXmlString(resourceListing)
      case false => toJsonString(resourceListing)
    }
    returnValue(request, response)
  }

  def getResource(path: String) = Action { implicit request =>
    val host: String = swaggerPlugin.config.host
    val apiListing: Swagger = getApiListing(path, host)
    val response: String = returnXml(request) match {
      case true => toXmlString(apiListing)
      case false => toJsonString(apiListing)
    }
    Option(response) match {
      case Some(help) => returnValue(request, help)
      case None =>
        val msg = new ErrorResponse(500, "api listing for path " + path + " not found")
        Logger("swagger").error(msg.message)
        if (returnXml(request)) {
          InternalServerError(ByteString(toXmlString(msg))).as(XML(Codec.utf_8))
        } else {
          InternalServerError(ByteString(toJsonString(msg))).as(JSON)
        }
    }
  }
}

trait SwaggerBaseApiController {

  def swaggerPlugin: SwaggerPlugin

  protected def returnXml(request: Request[_]) = request.path.contains(".xml")

  protected val AccessControlAllowOrigin = ("Access-Control-Allow-Origin", "*")

  /**
   * Get a list of all top level resources
   */
  protected def getResourceListing(host: String)(implicit requestHeader: RequestHeader): Swagger = {
    Logger("swagger").debug("ApiHelpInventory.getRootResources")
    val queryParams = (for((key, value) <- requestHeader.queryString) yield {
      (key, value.toList.asJava)
    }).asJava
    val cookies = (for(cookie <- requestHeader.cookies) yield {
      (cookie.name, cookie.value)
    }).toMap.asJava
    val headers = (for((key, value) <- requestHeader.headers.toMap) yield {
      (key, value.toList.asJava)
    }).asJava

    val f = new SpecFilter

    val specs = swaggerPlugin.apiListingCache.listing(host)

    swaggerPlugin.swaggerSpecFilter match {
      case Some(filter) => f.filter(specs, filter, queryParams, cookies, headers)
      case None => specs
    }
  }

  /**
   * Get detailed API/models for a given resource
   */
  protected def getApiListing(resourceName: String, host: String)(implicit requestHeader: RequestHeader): Swagger = {
    Logger("swagger").debug("ApiHelpInventory.getResource(%s)".format(resourceName))
    val f = new SpecFilter
    val queryParams = requestHeader.queryString.map {case (key, value) => key -> value.toList.asJava}
    val cookies = requestHeader.cookies.map {cookie => cookie.name -> cookie.value}.toMap.asJava
    val headers = requestHeader.headers.toMap.map {case (key, value) => key -> value.toList.asJava}.asJava
    val pathPart = resourceName

    val specs = swaggerPlugin.apiListingCache.listing(host)

    val clone = swaggerPlugin.swaggerSpecFilter match {
      case Some(filter) => f.filter(specs, filter, queryParams.asJava, cookies, headers)
      case None => specs
    }
    clone.setPaths(clone.getPaths.asScala.filter(_._1.startsWith(pathPart)).asJava)
    clone
  }

  // TODO: looks like this is broken for anything other than strings
  def toXmlString(data: Any): String = {
    if (data.getClass.equals(classOf[String])) {
      data.asInstanceOf[String]
    } else {
      val stringWriter = new StringWriter()
      stringWriter.toString
    }
  }

  protected def XmlResponse(data: Any) = {
    Results.Ok(toXmlString(data)).as(ContentTypes.XML(Codec.utf_8))
  }

  protected def returnValue(request: Request[_], obj: Any): Result = {
    val response = returnXml(request) match {
      case true => XmlResponse(obj)
      case false => JsonResponse(obj)
    }
    response.withHeaders(AccessControlAllowOrigin)
  }

  def toJsonString(data: Any): String = {
    if (data.getClass.equals(classOf[String])) {
      data.asInstanceOf[String]
    } else {
      Json.pretty(data.asInstanceOf[AnyRef])
    }
  }

  protected def JsonResponse(data: Any): Result = {
    Results.Ok(ByteString(toJsonString(data))).as(ContentTypes.JSON)
  }
}
