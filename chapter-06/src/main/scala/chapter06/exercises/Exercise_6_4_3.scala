package chapter06.exercises

import chapter06.ChatSchema.Schema

import scala.slick.jdbc.StaticQuery.interpolation

object Exercise_6_4_3 extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter06", driver = "org.h2.Driver")

  db withSession { implicit session =>

    populate

    def append(s: String) =
      sqlu"""UPDATE "message" SET "content" = CONCAT("content", $s)"""

    append(":Hello!").first

    messages.list.foreach(println(_))

    // Using, but not modifying, the method, restrict the update to messages from "HAL".
    def lookup(name: String) =
      sql"""select "id" from "user" where "user"."name" = '#${name}'"""

    // Example use:
    val halId = lookup("HAL").as[Long].first

    println(halId)

    val restrictedQuery = append("Eyup!") + """ WHERE "sender"=""" +? halId
    restrictedQuery.list

    messages.list.foreach(println(_))
  }
}