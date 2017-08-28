package chapter05.exercises

import chapter05.exercises.Exercise_5_6_3.PKs.{MessagePK, UserPK}
import chapter05.framework.Profile
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Exercise_5_6_3 extends App {

  object PKs {

    import slick.lifted.MappedTo

    case class UserPK(value: Long) extends AnyVal with MappedTo[Long]

    case class MessagePK(value: Long) extends AnyVal with MappedTo[Long]

  }

  trait Tables {
    this: Profile =>

    import profile.api._

    // Users in the user table have an id which will
    // be used as a foreign key in the message table.
    case class User(name: String, id: UserPK = UserPK(0L))

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[UserPK]("id", O.PrimaryKey, O.AutoInc)

      def name = column[String]("name")

      def * = (name, id).mapTo[User]
    }

    lazy val users = TableQuery[UserTable]
    lazy val insertUser = users returning users.map(_.id)

    // The message table, in which we represent the sender as
    // the key in the user table (rather than a String name
    // we've used up until this point)
    case class Message(
                        senderId: UserPK,
                        content: String,
                        id: MessagePK = MessagePK(0L))

    class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
      def id = column[MessagePK]("id", O.PrimaryKey, O.AutoInc)

      def senderId = column[UserPK]("sender")

      def content = column[String]("content")

      def * = (senderId, content, id).mapTo[Message]

      // Establish a FK relation:
      def sender = foreignKey("sender_fk", senderId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
    }

    lazy val messages = TableQuery[MessageTable]

    // The schema for both tables:
    lazy val ddl = users.schema ++ messages.schema
  }

  class Schema(val profile: JdbcProfile) extends Tables with Profile

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  val db = Database.forConfig("chapter05")

  val initalise =
    for {
      _ <- ddl.create
      _ <- messages ++= Seq(Message(UserPK(3000L), "Hello, HAL. Do you read me, HAL?"))
    } yield ()

  // Set up the database:
  exec(initalise)
}