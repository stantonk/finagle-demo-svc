package com.github.stantonk

import com.twitter.util.{Future, Await}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, DefaultHttpResponse, HttpResponseStatus}
import com.twitter.finagle.{Http, Service}
import org.jboss.netty.buffer.ChannelBuffers

object Main extends App {

  val service = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest): Future[HttpResponse] = {
      val resp = new DefaultHttpResponse(req.getProtocolVersion, HttpResponseStatus.OK)
      val b = ChannelBuffers.copiedBuffer("hello world".getBytes())
      resp.setContent(b)
      Future.value(resp)
    }
  }
  val server = Http.serve(":8080", service)
  Await.ready(server)
}
