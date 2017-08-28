package chapter05.exercises

import chapter05.framework.Profile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Exercise_5_6_6 extends App {

  object PKs {

    import slick.lifted.MappedTo

    case class MessagePK(value: Long) extends AnyVal with MappedTo[Long]

    case class UserPK(value: Long) extends AnyVal with MappedTo[Long]

  }

  trait Tables {
    this: Profile =>

    import PKs._
    import profile.api._

    case class User(name: String, id: UserPK = UserPK(0L))

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[UserPK]("id", O.PrimaryKey, O.AutoInc)

      def name = column[String]("name")

      def * = (name, id).mapTo[User]
    }

    lazy val users = TableQuery[UserTable]
    lazy val insertUser = users returning users.map(_.id)

    sealed trait Priority

    case object HighPriority extends Priority

    case object LowPriority extends Priority

    implicit val priorityType =
      MappedColumnType.base[Priority, String](
        flag => flag match {
          case HighPriority => "y"
          case LowPriority => "n"
        },
        ch => ch match {
          case "Y" | "y" | "+" | "high" => HighPriority
          case "N" | "n" | "-" | "lo" | "low" => LowPriority
        })

    case class Message(
                        senderId: UserPK,
                        content: String,
                        priority: Option[Priority] = None,
                        id: MessagePK = MessagePK(0L)
                      )

    class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
      def id = column[MessagePK]("id", O.PrimaryKey, O.AutoInc)

      def senderId = column[UserPK]("sender")

      def content = column[String]("content")

      def priority = column[Option[Priority]]("priority")

      def * = (senderId, content, priority, id).mapTo[Message]

      def sender = foreignKey("sender_fk", senderId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
    }

    lazy val messages = TableQuery[MessageTable]

    lazy val ddl = users.schema ++ messages.schema
  }


  class Schema(val profile: slick.jdbc.JdbcProfile) extends Tables with Profile

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  val db = Database.forConfig("chapter05")

  val program =
    for {
      _ <- ddl.create
      halId <- insertUser += User("HAL")
      daveId <- insertUser += User("Dave")
      _ <- messages ++= Seq(
        Message(daveId, "Hello, HAL. Do you read me, HAL?"),
        Message(halId, "Affirmative, Dave. I read you."),
        Message(daveId, "Open the pod bay doors, HAL."),
        Message(halId, "I'm sorry, Dave. I'm afraid I can't do that.", Some(HighPriority)))
      result <- messages.filter(_.priority === (HighPriority: Priority)).result
    } yield result


  println(s"query result ${exec(program)}")
}
