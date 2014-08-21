package com.github.stantonk

import com.twitter.util.{Duration, Future}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponseStatus}
import com.twitter.finagle.Service
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import com.google.gson.Gson
import com.twitter.finagle.http.service.RoutingService
import com.twitter.finagle.http.{RichHttp, Http, Response, Request}
import java.net.InetSocketAddress
import com.twitter.finagle.builder.ServerBuilder
import scala.slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource
import scala.collection.JavaConverters._


case class Person(id: Int, firstName: String, lastName: String, age: Int)

object Main extends App {
  val ds: MysqlDataSource = new MysqlDataSource()
  ds.setURL("jdbc:mysql://localhost/finagle_demo_svc")
  ds.setUser("finagle")
  ds.setPassword("finagle")
  val db = Database.forDataSource(ds)
  val gson = new Gson()

  def renderResponse(req : HttpRequest, content: String) : Response = {
    val resp = Response(req.getProtocolVersion, HttpResponseStatus.OK)
    resp.setContent(copiedBuffer(content.getBytes))
    resp
  }

  val personService = new Service[Request, Response] {
    implicit val getPersonResult = GetResult(r => Person(r.<<, r.<<, r.<<, r.<<))
    def apply(req: Request): Future[Response] = {
      val id : Option[Int] = req.params.getInt("id")

      var persons : List[Person] = null
      db withDynSession {
        if (id.isDefined) {
          persons = (Q[Int, Person] + "select id, first_name as firstName, last_name as lastName, age from person where id=?")(id.get).list
        } else {
          persons = Q.queryNA[Person]("select id, first_name as firstName, last_name as lastName, age from person").list
        }
      }

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
