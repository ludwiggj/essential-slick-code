package chapter06

import scala.concurrent.Await
import scala.concurrent.duration._

object MonadicJoinExamples extends App {

  def basicSchemaExamples() = {
    import schemas.ChatSchemaBasic.Schema

    val schema = new Schema(slick.jdbc.H2Profile)

    import schema._
    import profile.api._

    val db = Database.forConfig("chapter06")

    def exec[T](action: DBIO[T]): T =
      Await.result(db.run(action), 2 seconds)

    println(s"query result ${exec(populate)}")

    // Monadic join
    val q1 = for {
      msg <- messages
      usr <- msg.sender
    } yield (usr.name, msg.content)

    println(s"\nMessages (monadic for):\n${exec(q1.result)}")

    val q2 =
      messages flatMap { msg =>
        msg.sender.map { usr =>
          (usr.name, msg.content)
        }
      }

    println(s"\nMessages (monadic flatmap):\n${exec(q2.result)}")

    // If we don’t have a foreign key, we can use the same style and control the join ourselves:
    val q3 = for {
      msg <- messages
      usr <- users if usr.id === msg.senderId
    } yield (usr.name, msg.content)

    println(s"\nMessages (monadic no FK):\n${exec(q3.result)}")

    println(s"dropping tables ${exec(dropSchema)}")
  }

  def fullSchemaExamples() = {
    import schemas.ChatSchemaFull.Schema

    val schema = new Schema(slick.jdbc.H2Profile)

    import schema._
    import profile.api._

    val db = Database.forConfig("chapter06")

    def exec[T](action: DBIO[T]): T =
      Await.result(db.run(action), 2 seconds)

    println(s"query result ${exec(populate)}")

    val daveId = users.filter(_.name === "Dave").map(_.id)
    val halId = users.filter(_.name === "HAL").map(_.id)
    val elenaId = users.filter(_.name === "Elena").map(_.id)
    val frankId = users.filter(_.name === "Frank").map(_.id)

    val airLockId = rooms.filter(_.title === "Air Lock").map(_.id)
    val podId = rooms.filter(_.title === "Pod").map(_.id)

    // Dave's messages example:
    val davesMessages = for {
      dId <- daveId
      rId <- airLockId
      message <- messages
      user <- users
      room <- rooms
      if message.senderId === user.id &&
        message.roomId === room.id &&
        user.id === dId &&
        room.id === rId
    } yield (message.content, user.name, room.title)

    exec(davesMessages.result).foreach(println)

    val davesMessagesWithFKs = for {
      dId <- daveId
      rId <- airLockId
      message <- messages
      user <- message.sender
      room <- message.room
      if user.id === dId &&
        room.id === rId
    } yield (message.content, user.name, room.title)

    exec(davesMessagesWithFKs.result).foreach(result => println(result))
  }

  basicSchemaExamples()
  fullSchemaExamples()
}