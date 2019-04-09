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

package controllers

import java.io.StringWriter
import javax.inject.Inject
import javax.xml.bind.annotation._

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.swagger.config.FilterFactory
import io.swagger.core.filter.SpecFilter
import io.swagger.models.Swagger
import io.swagger.util.Json
import play.api.Logger
import play.api.http.{HeaderNames, HttpEntity}
import play.api.mvc._
import play.modules.swagger.ApiListingCache

import scala.collection.JavaConverters._

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

  def setType(`type`: String) = {}

  def getMessage: String = message

  def setMessage(message: String) = this.message = message
}

class ApiHelpController @Inject() (components: ControllerComponents, configuration: play.api.Configuration)
  extends AbstractController(components) with SwaggerBaseApiController {

  def getResources = Action {
    request =>
      implicit val requestHeader: RequestHeader = request
      val host: String = if (configuration.underlying.hasPath("swagger.api.host")) configuration.underlying.getString("swagger.api.host") else requestHeader.host
      val resourceListing: Swagger = getResourceListing(host)
      val response: String = if (returnXml(request))  toXmlString(resourceListing) else toJsonString(resourceListing)
      returnValue(request, response)
  }

  def getResource(path: String) = Action {
    request =>
      implicit val requestHeader: RequestHeader = request
      val host: String = if (configuration.underlying.hasPath("swagger.api.host")) configuration.underlying.getString("swagger.api.host") else requestHeader.host
      val apiListing: Swagger = getApiListing(path, host)
      val response: String = if (returnXml(request)) toXmlString(apiListing) else toJsonString(apiListing)
      Option(response) match {
        case Some(help) => returnValue(request, help)
        case None =>
          val msg = new ErrorResponse(500, "api listing for path " + path + " not found")
          Logger("swagger").error(msg.message)
          if (returnXml(request)) {
            InternalServerError.chunked(Source.single(toXmlString(msg).getBytes("UTF-8"))).as("application/xml")
          } else {
            InternalServerError.chunked(Source.single(toJsonString(msg).getBytes("UTF-8"))).as("application/json")
          }
      }
  }
}

trait SwaggerBaseApiController {

  protected def returnXml(request: Request[_]) = request.path.contains(".xml")

  protected val AccessControlAllowOrigin = ("Access-Control-Allow-Origin", "*")

  /**
   * Get a list of all top level resources
   */
  protected def getResourceListing(host: String)(implicit requestHeader: RequestHeader): Swagger = {
    Logger("swagger").debug("ApiHelpInventory.getRootResources")
    val docRoot = ""
    val queryParams = (for((key, value) <- requestHeader.queryString) yield {
      (key, value.toList.asJava)
    }).toMap
    val cookies = (for(cookie <- requestHeader.cookies) yield {
      (cookie.name, cookie.value)
    }).toMap
    val headers = (for((key, value) <- requestHeader.headers.toMap) yield {
      (key, value.toList.asJava)
    }).toMap

    val f = new SpecFilter
    val l: Option[Swagger] = ApiListingCache.listing(docRoot, host)

    val specs: Swagger = l match {
      case Some(m) => m
      case _ => new Swagger()
    }

    val hasFilter = Option(FilterFactory.getFilter)
    hasFilter match {
      case Some(filter) => f.filter(specs, FilterFactory.getFilter, queryParams.asJava, cookies.asJava, headers.asJava)
      case None => specs
    }


  }

  /**
   * Get detailed API/models for a given resource
   */
  protected def getApiListing(resourceName: String, host: String)(implicit requestHeader: RequestHeader): Swagger = {
    Logger("swagger").debug("ApiHelpInventory.getResource(%s)".format(resourceName))
    val docRoot = ""
    val f = new SpecFilter
    val queryParams = requestHeader.queryString.map {case (key, value) => key -> value.toList.asJava}
    val cookies = requestHeader.cookies.map {cookie => cookie.name -> cookie.value}.toMap.asJava
    val headers = requestHeader.headers.toMap.map {case (key, value) => key -> value.toList.asJava}
    val pathPart = resourceName

    val l: Option[Swagger] = ApiListingCache.listing(docRoot, host)
    val specs: Swagger = l match {
      case Some(m) => m
      case _ => new Swagger()
    }
    val hasFilter = Option(FilterFactory.getFilter)

    val clone = hasFilter match {
      case Some(filter) => f.filter(specs, FilterFactory.getFilter, queryParams.asJava, cookies, headers.asJava)
      case None => specs
    }
    clone.setPaths(clone.getPaths.asScala.filterKeys(_.startsWith(pathPart)).asJava)
    clone
  }

  def toXmlString(data: Any): String = {
    if (data.getClass.equals(classOf[String])) {
      data.asInstanceOf[String]
    } else {
      val stringWriter = new StringWriter()
      stringWriter.toString
    }
  }

  protected def XmlResponse(data: Any) = {
    val xmlValue = toXmlString(data)
    Results.Ok.chunked(Source.single(xmlValue.getBytes("UTF-8"))).as("application/xml")
  }

  protected def returnValue(request: Request[_], obj: Any): Result = {
    val response = if (returnXml(request)) XmlResponse(obj) else JsonResponse(obj)
    response.withHeaders(AccessControlAllowOrigin)
  }

  def toJsonString(data: Any): String = {
    if (data.getClass.equals(classOf[String])) {
      data.asInstanceOf[String]
    } else {
      Json.pretty(data.asInstanceOf[AnyRef])
    }
  }

  protected def JsonResponse(data: Any) = {
    val jsonBytes = toJsonString(data).getBytes("UTF-8")
    val source = Source.single(jsonBytes).map(ByteString.apply)
    Result (
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Streamed(source, Some(jsonBytes.length), Some("application/json"))
    ).as ("application/json")
  }
}
