package chapter05.exercises

import chapter05.ChatSchema.Schema

object Exercise_5_8_1 extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>
      populate

      // Display user and number of messages sent by that user
      val msgsPerUser =
        messages.join(users).on(_.senderId === _.id).
          groupBy { case (msg, user) => user.name }.
          map { case (name, group) => name -> group.length }

      msgsPerUser.run.foreach(println(_))

      // Modify the msgsPerUser query to return the counts for just those users with more than 2 messages
      val usersWhoHaveSentMoreThanTwoMessages =
        messages.join(users).on(_.senderId === _.id).
          groupBy { case (msg, user) => user.name }.
          map { case (name, group) => name -> group.length }.
          filter { case (_, length) => length > 2 }

      usersWhoHaveSentMoreThanTwoMessages.run.foreach(println(_))
  }
}