/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.http.{ContentTypes, HttpEntity, MimeTypes}
import play.api.libs.json.Json
import play.api.libs.ws.{BodyWritable, EmptyBody, InMemoryBody, WSClient}
import play.api.mvc._
import play.api.{Configuration, Logger}
import uk.gov.hmrc.http.HeaderCarrier.Config
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ProxyController @Inject() (ws: WSClient, config: Configuration, cc: ControllerComponents)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc) {

  val logger = Logger(this.getClass)

  implicit def writeableFor(implicit request: Request[AnyContent], codec: Codec): BodyWritable[AnyContent] =
    request.contentType match {
      case None => BodyWritable(_ => EmptyBody, request.contentType.getOrElse(""))
      case Some(ContentTypes.JSON) =>
        BodyWritable(a => InMemoryBody(codec.encode(Json.stringify(a.asJson.get))), request.contentType.getOrElse(""))
      case Some(ct) if ct.startsWith(MimeTypes.XML) =>
        BodyWritable(a => InMemoryBody(codec.encode(a.asXml.get.toString)), request.contentType.getOrElse(""))
      case Some(ContentTypes.FORM) =>
        BodyWritable(
          a =>
            InMemoryBody(
              codec.encode(
                (a.asFormUrlEncoded.get flatMap (item =>
                  item._2.map(c => s"""${item._1}=${URLEncoder.encode(c, "UTF-8")}""")
                )).mkString("&")
              )
            ),
          request.contentType.getOrElse("")
        )
      case _ => throw new UnsupportedOperationException()
    }

  def proxyPassTo(url: String): Action[AnyContent] = Action.async { implicit request =>
    targetUrlFor(request.path) match {
      case Some(targetUrl) =>
        logger.info("Sending upstream proxy request " + targetUrl)
        val requestContentType: Seq[(String, String)] =
          request.headers.get(CONTENT_TYPE).map((CONTENT_TYPE, _)).toSeq
        val upstreamRequest = ws
          .url(targetUrl)
          .withMethod(request.method)
          .withQueryStringParameters(request.queryString.toSeq.flatMap({ case (k, sv) => sv.map(v => (k, v)) }): _*)
          .withHttpHeaders(hc.withExtraHeaders(requestContentType: _*).headersForUrl(Config())(targetUrl): _*)
          .withBody(request.body)
        upstreamRequest
          .stream()
          .map { response =>
            val responseContentType = response.headers.get(CONTENT_TYPE).flatMap(_.headOption)
            val length = response.headers.get(CONTENT_LENGTH).flatMap(_.headOption).map(_.toLong)
            logger.info("Got upstream response " + response.status)
            Results
              .Status(response.status)
              .sendEntity(HttpEntity.Streamed(response.bodyAsSource, length, responseContentType))
              .withHeaders(response.headers.toSeq.flatMap({ case (k, sv) => sv.map(v => (k, v)) }): _*)
          }
      case None =>
        Future.successful(
          BadRequest(s"Could not construct target URL for ${request.path}, probably missing configuration.")
        )
    }
  }

  def targetUrlFor(path: String): Option[String] =
    path.split("/").filterNot(_.isEmpty).headOption.flatMap { prefix =>
      Try(config.underlying.getConfig(s"microservice.services.$prefix")).toOption.flatMap { sc =>
        for {
          protocol <- Try(sc.getString("protocol")).toOption
          host     <- Try(sc.getString("host")).toOption
          port     <- Try(sc.getInt("port")).toOption
        } yield s"$protocol://$host:$port${if (path.startsWith("/")) path else s"/$path"}"
      }
    }

}
