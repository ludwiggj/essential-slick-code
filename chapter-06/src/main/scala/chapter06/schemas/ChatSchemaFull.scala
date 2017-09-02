package chapter06.schemas

import chapter06.Profile
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global

object ChatSchemaFull {

  trait Tables {
    this: Profile =>

    import profile.api._

    case class User(name: String, id: Long = 0L)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

      def name = column[String]("name")

      def * = (name, id).mapTo[User]
    }

    lazy val users = TableQuery[UserTable]
    lazy val insertUser = users returning users.map(_.id)

    case class Message(
                        senderId: Long,
                        content: String,
                        roomId: Option[Long] = None,
                        toId: Option[Long] = None,
                        id: Long = 0L
                      )

    class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

      def senderId = column[Long]("sender")

      def content = column[String]("content")

      def roomId = column[Option[Long]]("roomId") // in a particular room? or broadcast?

      def toId = column[Option[Long]]("to") // direct message?

      def * = (senderId, content, roomId, toId, id).mapTo[Message]

      def sender = foreignKey("msg_sender_fk", senderId, users)(_.id)

      def to = foreignKey("msg_to_fk", toId, users)(_.id.?)

      def room = foreignKey("msg_room_fk", roomId, rooms)(_.id.?)
    }

    lazy val messages = TableQuery[MessageTable]
    lazy val insertMessage = messages returning messages.map(_.id)

    case class Room(title: String, id: Long = 0L)

    class RoomTable(tag: Tag) extends Table[Room](tag, "room") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

      def title = column[String]("title")

      def * = (title, id).mapTo[Room]
    }

    lazy val rooms = TableQuery[RoomTable]
    lazy val insertRoom = rooms returning rooms.map(_.id)

    lazy val ddl = users.schema ++ messages.schema ++ rooms.schema

    // Sample data set
    def populate = {
      val program = for {
        _ <- ddl.create
        daveId <- insertUser += User("Dave")
        halId <- insertUser += User("HAL")
        airLockId <- insertRoom += Room("Air Lock")
        _ <- insertRoom += Room("Pod Bay")

        // Half the messages will be in the air lock room...
        _ <- insertMessage += Message(daveId, "Hello, HAL. Do you read me, HAL?", Some(airLockId), toId = None)
        _ <- insertMessage += Message(halId, "Affirmative, Dave. I read you.", Some(airLockId), toId = Some(daveId))

        // ...and half will not be in room:
        _ <- insertMessage += Message(daveId, "Open the pod bay doors, HAL.")
        _ <- insertMessage += Message(halId, "I'm sorry, Dave. I'm afraid I can't do that.")

        msgs <- messages.result
      } yield (msgs)

      program
    }
  }

  case class Schema(val profile: JdbcProfile) extends Tables with Profile

}