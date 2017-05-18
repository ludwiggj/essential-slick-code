package chapter06.exercises

import chapter06.ChatSchema.Schema

import scala.slick.jdbc.StaticQuery.interpolation

object Exercise_6_4_1 extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter06", driver = "org.h2.Driver")

  db withSession { implicit session =>

    populate

    //  We’re building a web site that allows searching for users by their email address:
    def lookup(email: String) =
      sql"""select "id" from "user" where "user"."email" = '#${email}'"""

    // Example use:
    println(
      lookup("dave@example.org").as[Long].firstOption
    )

    // What the problem with this code?

    // #$ does not escape input.
    // A user could use a carefully crafted email address to do evil.

    // Evil...
    lookup(""" ';DROP TABLE "user";--- """).as[Long].list

    // This will produce: .JdbcSQLException: Table "user" not found;
    println(
      lookup("dave@example.org").as[Long].firstOption
    )
  }
}