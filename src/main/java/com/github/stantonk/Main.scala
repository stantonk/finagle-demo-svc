package com.github.stantonk

import com.twitter.util.{Future, Await}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, DefaultHttpResponse, HttpResponseStatus}
import com.twitter.finagle.{Http, Service}
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.BeanListHandler
import scala.collection.JavaConverters._
import scala.beans.BeanInfo
import com.google.gson.Gson

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

  def renderResponse(req : HttpRequest, content: String) : DefaultHttpResponse = {
    val resp = new DefaultHttpResponse(req.getProtocolVersion, HttpResponseStatus.OK)
    resp.setContent(copiedBuffer(content.getBytes))
    resp
  }

  val service = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest): Future[HttpResponse] = {
      val qr = new QueryRunner(ds)
      val h = new BeanListHandler[Person](classOf[Person])

      val persons = qr.query("SELECT id, first_name as firstName, last_name as lastName, age from person", h).asScala
//      for (p <- persons)
//        println(p)

      Future.value(renderResponse(req, gson.toJson(persons.asJava)))
    }
  }
  val server = Http.serve(":8080", service)
  Await.ready(server)
}
