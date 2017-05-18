package chapter06.exercises

import chapter06.ChatSchema.Schema

import scala.slick.jdbc.StaticQuery.interpolation

object Exercise_6_4_2 extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter06", driver = "org.h2.Driver")

  db withSession { implicit session =>

    populate

    // When we constructed our sensitive query, we used +? to include a String in our query.
    val query = sql""" select "id" from "message" """.as[Long]

    println((query + """ WHERE "content" NOT LIKE """ +? "%Dave%").list)

    println((query + """ WHERE "content" NOT LIKE """ +? """ ';DROP TABLE "message";--- """).list)

    println(messages.list)

    // It looks as if we could have used regular string interpolation instead:
    // Why didn’t we do that?

    val pattern = "%Dave%"

    println((query + s""" WHERE "content" NOT LIKE '$pattern'""").list)

    val evilPattern = """ ';DROP TABLE "message";--- """
    println((query + s""" WHERE "content" NOT LIKE '$evilPattern'""").list)

    println(messages.list)
  }
}