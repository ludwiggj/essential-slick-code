package chapter06

import org.joda.time.DateTime
import scala.slick.jdbc._
import scala.slick.jdbc.{ StaticQuery => Q }
import ChatSchema._
import java.sql.Timestamp

object UpdateExample extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)
  import schema._, profile.simple._
  def db = Database.forURL("jdbc:h2:mem:chapter06", driver="org.h2.Driver")

  db.withSession { implicit session =>

    populate

    import Q.interpolation

    // Simple update example:
    val char = "!"
    val query = sqlu"""UPDATE "message" SET "content" = CONCAT("content", $char)"""
    val exclaimedRows = query.first
    println(s"Exclamation mark added to $exclaimedRows rows")

    println(messages.list)

    // Using + and +?
    val pattern = "%?"
    val sensitive =  query + """ WHERE "content" NOT LIKE """ +? pattern
    val sensitiveRows = sensitive.first
    println(s"Exclamation mark added sensitively to $sensitiveRows rows")

    println(messages.list)

    // Using DateTime as an example of a custom type

    implicit val SetDateTime = SetParameter[DateTime](
      //(dt, pp) => pp.setTimestamp(new Timestamp(dt.getMillis))
       (dt, pp) => pp >> new Timestamp(dt.getMillis)
     )

    val now = sqlu"""UPDATE "message" SET "ts" = """ +? DateTime.now

    now.first

    println(messages.list)
  }
}