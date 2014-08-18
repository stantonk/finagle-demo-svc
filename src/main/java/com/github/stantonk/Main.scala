package com.github.stantonk

import com.twitter.util.{Duration, Future}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, DefaultHttpResponse, HttpResponseStatus}
import com.twitter.finagle.Service
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.BeanListHandler
import scala.collection.JavaConverters._
import scala.beans.BeanInfo
import com.google.gson.Gson
import com.twitter.finagle.http.service.RoutingService
import com.twitter.finagle.http.{RichHttp, Http, Response, Request}
import java.net.InetSocketAddress
import com.twitter.finagle.builder.ServerBuilder
import scala.collection.mutable
import org.slf4j.LoggerFactory

@BeanInfo
class Person {
  var id: Int = 0
  var firstName: String = ""
  var lastName: String = ""
  var age: Int = 0

  override def toString: String = {
    "Person{id=" + id +
    ",firstName=" + firstName +
    ",lastName=" + lastName +
    ",age=" + age +
    "}"
  }
}

object Main extends App {
  val gson = new Gson()
  val ds = new MysqlDataSource()
  ds.setURL("jdbc:mysql://localhost/stantonk")
  ds.setUser("stantonk")
  ds.setPassword("stantonk")

  def renderResponse(req : HttpRequest, content: String) : Response = {
    val resp = Response(req.getProtocolVersion, HttpResponseStatus.OK)
    resp.setContent(copiedBuffer(content.getBytes))
    resp
  }

  val personService = new Service[Request, Response] {
    def apply(req: Request): Future[Response] = {
      val id : Option[Int] = req.params.getInt("id")

      val qr = new QueryRunner(ds)
      val h = new BeanListHandler[Person](classOf[Person])
      val persons = qr.query("SELECT id, first_name as firstName, last_name as lastName, age from person", h).asScala
      //      for (p <- persons)
      //        println(p)

      Future.value(renderResponse(req, gson.toJson(persons.asJava)))
    }
  }

  val routingService = RoutingService.byPath {
    case "/persons" => personService
  }

  ServerBuilder()
    .codec(RichHttp[Request](Http()))
    .hostConnectionMaxLifeTime(Duration.fromSeconds(5*60))
    .readTimeout(Duration.fromSeconds(2*60))
    .name("servicename")
    .bindTo(new InetSocketAddress(8080))
    .build(routingService)
}
