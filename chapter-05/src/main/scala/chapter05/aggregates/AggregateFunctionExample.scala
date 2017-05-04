package chapter05.aggregates

import chapter05.ChatSchema.Schema
import org.joda.time._

object AggregateFunctionExample extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>

      populate

      // Count:
      val numRows: Int = messages.length.run
      println(s"Total messages: $numRows")

      // Number of message senders:
      val senders: Int = messages.map(_.senderId).countDistinct.run
      println(s"Unique message senders: $senders")

      // First message date:
      val first: Option[DateTime] = messages.map(_.ts).min.run
      println(s"First sent: $first")

      // Last message date:
      val last: Option[DateTime] = messages.map(_.ts).max.run
      println(s"Last sent: $last")
  }
}