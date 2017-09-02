package chapter06.schemas

import chapter06.Profile
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global

object ChatSchemaBasic {

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

    case class Message(senderId: Long, content: String, id: Long = 0L)

    class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

      def senderId = column[Long]("sender")

      def content = column[String]("content")

      def * = (senderId, content, id).mapTo[Message]

      def sender = foreignKey("msg_sender_fk", senderId, users)(_.id)
    }

    lazy val messages = TableQuery[MessageTable]

    lazy val ddl = users.schema ++ messages.schema

    // Sample data set
    def populate = {

      val program = for {
        _ <- ddl.create
        daveId <- insertUser += User("Dave")
        halId <- insertUser += User("HAL")
        elenaId <- insertUser += User("Elena")
        frankId <- insertUser += User("Frank")
        a <- messages ++= Seq(
          Message(daveId, "Hello, HAL. Do you read me, HAL?"),
          Message(halId, "Affirmative, Dave. I read you."),
          Message(daveId, "Open the pod bay doors, HAL."),
          Message(halId, "I'm sorry, Dave. I'm afraid I can't do that."))
        b <- messages ++= Seq(
          Message(frankId, "Well, whaddya think?"),
          Message(daveId, "I'm not sure, what do you think?"))
        c <- messages ++= Seq(
          Message(frankId, "Are you thinking what I'm thinking?"),
          Message(daveId, "Maybe"))
        d <- messages ++= Seq(
          Message(halId, "I am a HAL 9000 computer."),
          Message(halId, "I became operational at the H.A.L. plant in Urbana, Illinois on the 12th of January 1992."))
      } yield (a, b, c, d)

      program
    }

    def dropSchema = for {
      _ <- ddl.drop
    } yield ()
  }

  case class Schema(val profile: JdbcProfile) extends Tables with Profile

}