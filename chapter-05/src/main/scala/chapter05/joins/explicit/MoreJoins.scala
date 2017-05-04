package chapter05.joins.explicit

import chapter05.ChatSchema._

object MoreJoins extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>
      populate

      val daveId: PK[UserTable] = users.filter(_.name === "Dave").map(_.id).first
      val airLockId: PK[RoomTable] = rooms.filter(_.title === "Air Lock").map(_.id).first

      // Dave's messages example:
      val davesMessages = for {
        message <- messages
        user <- users
        room <- rooms
        if message.senderId === user.id &&
          message.roomId === room.id &&
          user.id === daveId &&
          room.id === airLockId
      } yield (message.content, user.name, room.title)

      davesMessages.run.foreach(result => println(result))

      val davesMessagesWithFKs = for {
        message <- messages
        user <- message.sender
        room <- message.room
        if user.id === daveId &&
          room.id === airLockId
      } yield (message.content, user.name, room.title)

      davesMessagesWithFKs.run.foreach(result => println(result))
  }
}