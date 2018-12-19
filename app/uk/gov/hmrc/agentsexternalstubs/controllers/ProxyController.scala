package uk.gov.hmrc.agentsexternalstubs.controllers

import java.net.URLEncoder

import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import play.api.http.{ContentTypes, HttpEntity, MimeTypes, Writeable}
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.libs.json.Json
import play.api.libs.ws.{StreamedResponse, WSClient}
import play.api.mvc._
import play.api.{Configuration, Logger}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ProxyController @Inject()(ecp: ExecutionContextProvider, ws: WSClient, config: Configuration)
    extends BaseController {

  implicit val ec: ExecutionContext = ecp.get()

  implicit def writeableFor(implicit request: Request[AnyContent], codec: Codec): Writeable[AnyContent] =
    request.contentType match {
      case None                    => Writeable(_ => ByteString.empty, None)
      case Some(ContentTypes.JSON) => Writeable(a => codec.encode(Json.stringify(a.asJson.get)), request.contentType)
      case Some(ct) if ct.startsWith(MimeTypes.XML) =>
        Writeable(a => codec.encode(a.asXml.get.toString), request.contentType)
      case Some(ContentTypes.FORM) =>
        Writeable(
          a =>
            codec.encode((a.asFormUrlEncoded.get flatMap (item =>
              item._2.map(c => s"""${item._1}=${URLEncoder.encode(c, "UTF-8")}"""))).mkString("&")),
          request.contentType
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
          .map {
            case StreamedResponse(response, body) =>
              val responseContentType = response.headers.get("Content-Type").flatMap(_.headOption)
              val length = response.headers.get("Content-Length").flatMap(_.headOption).map(_.toLong)
              Logger.info("Got upstream response " + response.status)
              Results
                .Status(response.status)
                .sendEntity(HttpEntity.Streamed(body, length, responseContentType))
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
