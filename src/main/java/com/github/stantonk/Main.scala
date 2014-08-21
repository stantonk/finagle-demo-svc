package com.github.stantonk

import com.twitter.util.{Duration, Future}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponseStatus}
import com.twitter.finagle.Service
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import com.google.gson.GsonBuilder
import com.twitter.finagle.http.service.RoutingService
import com.twitter.finagle.http._
import java.net.InetSocketAddress
import com.twitter.finagle.builder.ServerBuilder
import org.slf4j.{LoggerFactory, Logger}
import scala.slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource
import scala.collection.JavaConverters._
import org.jboss.netty.handler.codec.http.HttpMethod._

case class Person(id: Int, firstName: String, lastName: String, age: Int)

object Main extends App {
  val logger : Logger = LoggerFactory.getLogger("rootLogger")
  val ds: MysqlDataSource = new MysqlDataSource()
  ds.setURL("jdbc:mysql://localhost/finagle_demo_svc")
  ds.setUser("finagle")
  ds.setPassword("finagle")
  val db = Database.forDataSource(ds)
  val gson = new GsonBuilder().serializeNulls().create()

  def renderResponse(req : HttpRequest, content: String) : Response = {
    val resp = Response(req.getProtocolVersion, HttpResponseStatus.OK)
    resp.setContent(copiedBuffer(content.getBytes))
    resp
  }

  val personGetService = new Service[Request, Response] {
    implicit val getPersonResult = GetResult(r => Person(r.<<, r.<<, r.<<, r.<<))
    def apply(req: Request): Future[Response] = {
      val id : Option[Int] = req.params.getInt("id")

      val persons: List[Person] = db withDynSession {
        if (id.isDefined) {
          (Q[Int, Person] + "select id, first_name as firstName, last_name as lastName, age from person where id=?")(id.get).list
        } else {
          Q.queryNA[Person]("select id, first_name as firstName, last_name as lastName, age from person").list
        }
      }

      Future.value(renderResponse(req, gson.toJson(persons.asJava)))
    }
  }

  val personPostService = new Service[Request, Response] {
    def apply(req: Request): Future[Response] = {
      val p : Person = req withReader { reader => gson.fromJson(reader, classOf[Person]) }
      logger.info("new person: {}", p)
        db withDynSession {
          (Q.u + "insert into person (first_name, last_name, age) values (" +? p.firstName +
            "," +? p.lastName + "," +? p.age + ")").execute
      }
      val response = req.response
      response.status = Status.Accepted
      Future.value(response)
    }
  }

  //TODO: next, get HTTP Methods as part of routing, build CRUD (GET,POST,PUT,DELETE) on a Person object
  val routingService = RoutingService.byMethodAndPath {
    case (GET, "/persons") => personGetService //TODO: get person id from route instead of querystring
    case (POST, "/persons") => personPostService
  }

  ServerBuilder()
    .codec(RichHttp[Request](Http()))
    .hostConnectionMaxLifeTime(Duration.fromSeconds(5*60))
    .readTimeout(Duration.fromSeconds(2*60))
    .name("servicename")
    .bindTo(new InetSocketAddress(8080))
    .build(routingService)
}
