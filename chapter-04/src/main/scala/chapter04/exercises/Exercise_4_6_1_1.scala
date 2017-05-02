package chapter04.exercises

import chapter04.framework.Profile

// Messages can be high priority or low priority.
// The database value for high priority messages will be: y, Y, +, or high.
// For low priority messages the value will be: n, N, -, lo, or low.

// Model with a sum type

sealed trait Priority

case object LowPriority extends Priority

case object HighPriority extends Priority

object Exercise_4_6_1_1 extends App {

  trait Tables {
    this: Profile =>

    import profile.simple._

    implicit val stringPriorityType =
      MappedColumnType.base[Priority, String](
        pr => pr match {
          case LowPriority => "n"
          case HighPriority => "y"
        },
        //        str => str match {
        //          case hp if List("y", "Y", "+", "high").contains(hp) => HighPriority
        //          case lp if List("n", "N", "-", "lo", "low").contains(lp) => LowPriority
        //        }
        str => str match {
          case "y" | "Y" | "+" | "high" => HighPriority
          case "n" | "N" | "-" | "lo" | "low" => LowPriority
        }
      )

    case class User(id: Option[Long], name: String, email: Option[String] = None)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

      def name = column[String]("name")

      def email = column[Option[String]]("email")

      def * = (id.?, name, email) <> (User.tupled, User.unapply)
    }

    lazy val users = TableQuery[UserTable]
    lazy val insertUser = users returning users.map(_.id)

    case class Message(senderId: Long, content: String, priority: Option[Priority], id: Long = 0L)

    class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

      def senderId = column[Long]("sender")

      def content = column[String]("content")

      def priority = column[Option[Priority]]("priority")

      def * = (senderId, content, priority, id) <> (Message.tupled, Message.unapply)

      def sender = foreignKey("sender_fk", senderId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
    }

    lazy val messages = TableQuery[MessageTable]
    lazy val insertMessage = messages returning messages.map(_.id)
  }

  class Schema(val profile: scala.slick.driver.JdbcProfile) extends Tables with Profile

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter04", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>

      (users.ddl ++ messages.ddl).create

      val daveId: Long = insertUser += User(None, "Dave", Some("dave@example.org"))

      println(users.list)

      insertMessage += Message(daveId, "Message with no priority", None)
      insertMessage += Message(daveId, "Message with high priority", Some(HighPriority))
      insertMessage += Message(daveId, "Message with low priority", Some(LowPriority))

      println(messages.list)

      println(messages.filter(_.priority === (HighPriority:Priority)).run)
  }
}