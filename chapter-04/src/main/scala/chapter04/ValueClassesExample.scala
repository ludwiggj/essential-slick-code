package chapter04

import java.sql.Timestamp

import chapter04.framework.Profile
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC

object ValueClassesExample extends App {

  object PKs {

    import scala.slick.lifted.MappedTo

    case class MessagePK(value: Long) extends AnyVal with MappedTo[Long]

    case class UserPK(value: Long) extends AnyVal with MappedTo[Long]

  }

  trait Tables {
    this: Profile =>

    import profile.simple._
    import PKs._

    implicit val jodaDateTimeType =
      MappedColumnType.base[DateTime, Timestamp](
        dt => new Timestamp(dt.getMillis),
        ts => new DateTime(ts.getTime, UTC))

    case class User(name: String, id: UserPK = UserPK(0L))

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[UserPK]("id", O.PrimaryKey, O.AutoInc)

      def name = column[String]("name")

      def * = (name, id) <> (User.tupled, User.unapply)
    }

    lazy val users = TableQuery[UserTable]
    lazy val insertUser = users returning users.map(_.id)

    case class Message(
                        senderId: UserPK,
                        content: String,
                        ts: DateTime,
                        id: MessagePK = MessagePK(0L))

    class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
      def id = column[MessagePK]("id", O.PrimaryKey, O.AutoInc)

      def senderId = column[UserPK]("sender")

      def content = column[String]("content")

      def ts = column[DateTime]("ts")

      def * = (senderId, content, ts, id) <> (Message.tupled, Message.unapply)

      def sender = foreignKey("sender_fk", senderId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
    }

    lazy val messages = TableQuery[MessageTable]
  }


  class Schema(val profile: scala.slick.driver.JdbcProfile) extends Tables with Profile

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._, profile.simple._
  import PKs._

  def db = Database.forURL("jdbc:h2:mem:chapter04", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>

      (messages.ddl ++ users.ddl).create

      // Users:
      val halId = insertUser += User("HAL")
      val daveId = insertUser += User("Dave")

      // Insert the conversation, which took place in Feb, 2001:
      val start = new DateTime(2001, 2, 17, 10, 22, 50)

      messages ++= Seq(
        Message(daveId, "Hello, HAL. Do you read me, HAL?", start),
        Message(halId, "Affirmative, Dave. I read you.", start plusSeconds 2),
        Message(daveId, "Open the pod bay doors, HAL.", start plusSeconds 4),
        Message(halId, "I'm sorry, Dave. I'm afraid I can't do that.", start plusSeconds 6))

      // Won't compile:
      // users.filter(_.id === 6L).run

      // Buggy lookup of a sender
      val id: MessagePK = messages.filter(_.senderId === halId).map(_.id).first

      // This won't compile, as senderId and id are now different types
      // val rubbish = messages.filter(_.senderId === id)
  }
}