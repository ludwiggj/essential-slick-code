package chapter05.joins.old

import chapter05.ChatSchema._

object ImplicitJoinsExample extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>
      populate

      // Using the sender foreign key
      val implicitJoin = for {
        msg <- messages
        usr <- msg.sender
      } yield (usr.name, msg.content)

      implicitJoin.run.foreach(result => println(result))

      // Explicitly controlling the table join
      val implicitJoinControllingTableJoin = for {
        msg <- messages
        usr <- users
        if usr.id === msg.senderId
      } yield (usr.name, msg.content)

      implicitJoinControllingTableJoin.run.foreach(result => println(result))

  }
}