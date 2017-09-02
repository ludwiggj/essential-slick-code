package chapter06.schemas

import java.sql.Timestamp

import chapter06.Profile
import org.joda.time.DateTime
import org.joda.time.DateTimeZone._
import slick.jdbc.JdbcProfile
import slick.lifted.MappedTo

import scala.concurrent.ExecutionContext.Implicits.global

object ChatSchemaAggregates {

  case class PK[A](value: Long) extends AnyVal with MappedTo[Long]

  trait Tables {
    this: Profile =>

    import profile.api._

    case class User(name: String, email: Option[String] = None, id: PK[UserTable] = PK(0))

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id    = column[PK[UserTable]]("id", O.AutoInc, O.PrimaryKey)
      def name  = column[String]("name")
      def email = column[Option[String]]("email")

      def * = (name, email, id).mapTo[User]
    }

    lazy val users = TableQuery[UserTable]
    lazy val insertUser = users returning users.map(_.id)

    case class Room(title: String, id: PK[RoomTable] = PK(0L))

    class RoomTable(tag: Tag) extends Table[Room](tag, "room") {
      def id    = column[PK[RoomTable]]("id", O.PrimaryKey, O.AutoInc)
      def title = column[String]("title")
      def * = (title, id).mapTo[Room]
    }

    lazy val rooms = TableQuery[RoomTable]
    lazy val insertRoom = rooms returning rooms.map(_.id)

    implicit val jodaDateTimeType =
      MappedColumnType.base[DateTime, Timestamp](
        dt => new Timestamp(dt.getMillis),
        ts => new DateTime(ts.getTime, UTC))

    case class Message(senderId: PK[UserTable],
                       content: String,
                       ts: DateTime,
                       roomId: Option[PK[RoomTable]] = None,
                       toId: Option[PK[UserTable]]   = None,
                       id: PK[MessageTable]          = PK(0L) )

    class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
      def id       = column[PK[MessageTable]]("id", O.PrimaryKey, O.AutoInc)
      def senderId = column[PK[UserTable]]("sender")
      def content  = column[String]("content")
      def ts       = column[DateTime]("ts")
      def roomId   = column[Option[PK[RoomTable]]]("room") // in a particular room? or broadcast?
      def toId     = column[Option[PK[UserTable]]]("to")   // direct message?
      def * = (senderId, content, ts, roomId, toId, id).mapTo[Message]

      def sender = foreignKey("msg_sender_fk", senderId, users)(_.id)
      def to     = foreignKey("msg_to_fk", toId, users)(_.id.?)
      def room   = foreignKey("msg_room_fk", roomId, rooms)(_.id.?)
    }

    lazy val messages = TableQuery[MessageTable]

    lazy val ddl = users.schema ++ rooms.schema ++ messages.schema

    // Sample data set
    def populate = {

      // Insert the conversation, which took place in Feb, 2001:
      val airLockConversation = new DateTime(2001, 2, 17, 10, 22, 50)
      // A few messages in the Pod:
      val podConversation = new DateTime(2001, 2, 16, 20, 55, 0)
      //HAL monologue
      val halMonologue = new DateTime(2001, 2, 17, 22, 50, 0)

      val program = for {
        _          <- ddl.create
        daveId     <- insertUser += User("Dave", Some("dave@example.org"))
        halId      <- insertUser += User("HAL")
        elenaId    <- insertUser += User("Elena", Some("elena@example.org"))
        frankId    <- insertUser += User("Frank", Some("frank@example.org"))
        airLockId  <- insertRoom += Room("Air Lock")
        podId      <- insertRoom += Room("Pod")
        quartersId <- insertRoom += Room("Crew Quarters")
        a          <- messages ++= Seq(
                       Message(daveId, "Hello, HAL. Do you read me, HAL?",             airLockConversation,               Some(airLockId)),
                       Message(halId,  "Affirmative, Dave. I read you.",               airLockConversation plusSeconds 2, Some(airLockId)),
                       Message(daveId, "Open the pod bay doors, HAL.",                 airLockConversation plusSeconds 4, Some(airLockId)),
                       Message(halId,  "I'm sorry, Dave. I'm afraid I can't do that.", airLockConversation plusSeconds 6, Some(airLockId)))
        b          <- messages ++= Seq(
                       Message(frankId, "Well, whaddya think?", podConversation, Some(podId)),
                       Message(daveId, "I'm not sure, what do you think?", podConversation plusSeconds 4, Some(podId)))
        c          <- messages ++= Seq(
                       Message(frankId, "Are you thinking what I'm thinking?", podConversation plusSeconds 6, Some(podId), toId=Some(daveId)),
                       Message(daveId, "Maybe", podConversation plusSeconds 8, Some(podId), toId=Some(frankId)))
        d          <-  messages ++= Seq(
                       Message(halId, "I am a HAL 9000 computer.",                                                                 halMonologue              , None, toId=None),
                       Message(halId, "I became operational at the H.A.L. plant in Urbana, Illinois on the 12th of January 1992.", halMonologue plusSeconds 4, None, toId=None))
      } yield (a, b,c,d)

      program
    }
  }

  case class Schema(val profile: JdbcProfile) extends Tables with Profile
}