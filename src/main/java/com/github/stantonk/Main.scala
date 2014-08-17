package com.github.stantonk

import com.twitter.util.{Future, Await}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, DefaultHttpResponse, HttpResponseStatus}
import com.twitter.finagle.{Http, Service}
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer

object Main extends App {

  def renderResponse(req : HttpRequest, content: String) : DefaultHttpResponse = {
    val resp = new DefaultHttpResponse(req.getProtocolVersion, HttpResponseStatus.OK)
    resp.setContent(copiedBuffer(content.getBytes))
    resp
  }

  val service = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest): Future[HttpResponse] = {
      Future.value(renderResponse(req, "Hello World!"))
    }
  }
  val server = Http.serve(":8080", service)
  Await.ready(server)
}
