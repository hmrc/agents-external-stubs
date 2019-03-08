package uk.gov.hmrc.agentsexternalstubs.controllers

import java.net.URLEncoder

import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import play.api.http.{ContentTypes, HttpEntity, MimeTypes, Writeable}
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.libs.json.Json
import play.api.libs.ws.{BodyWritable, EmptyBody, InMemoryBody, WSClient}
import play.api.mvc._
import play.api.{Configuration, Logger}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ProxyController @Inject()(ws: WSClient, config: Configuration, cc: ControllerComponents)(
  implicit executionContext: ExecutionContext)
    extends BackendController(cc) {

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
            InMemoryBody(codec.encode((a.asFormUrlEncoded.get flatMap (item =>
              item._2.map(c => s"""${item._1}=${URLEncoder.encode(c, "UTF-8")}"""))).mkString("&"))),
          request.contentType.getOrElse("")
        )
      case _ => throw new UnsupportedOperationException()
    }

  def proxyPassTo(url: String): Action[AnyContent] = Action.async { implicit request =>
    targetUrlFor(request.path) match {
      case Some(targetUrl) =>
        Logger.info("Sending upstream proxy request " + targetUrl.toString)
        val requestContentType: Seq[(String, String)] =
          request.headers.get("Content-Type").map(("Content-Type", _)).toSeq
        val upstreamRequest = ws
          .url(targetUrl)
          .withMethod(request.method)
          .withQueryString(request.queryString.toSeq.flatMap({ case (k, sv) => sv.map(v => (k, v)) }): _*)
          .withHeaders(hc.withExtraHeaders(requestContentType: _*).headers: _*)
          .withBody(request.body)
        upstreamRequest
          .stream()
          .map { response =>
            val responseContentType = response.headers.get("Content-Type").flatMap(_.headOption)
            val length = response.headers.get("Content-Length").flatMap(_.headOption).map(_.toLong)
            Logger.info("Got upstream response " + response.status)
            Results
              .Status(response.status)
              .sendEntity(HttpEntity.Streamed(response.bodyAsSource, length, responseContentType))
              .withHeaders(response.headers.toSeq.flatMap({ case (k, sv) => sv.map(v => (k, v)) }): _*)
          }
      case None =>
        Future.successful(
          BadRequest(s"Could not construct target URL for ${request.path}, probably missing configuration."))
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
