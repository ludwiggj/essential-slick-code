package chapter04.representations.tablescolumns

import chapter04.framework.Profile

object ForeignKeyExample extends App {

  trait Tables {
    this: Profile =>

    import profile.simple._

    case class User(name: String, id: Long = 0L)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

      def name = column[String]("name")

      def * = (name, id) <> (User.tupled, User.unapply)
    }

    lazy val users = TableQuery[UserTable]

    lazy val insertUser = users returning users.map(_.id)

    case class Message(senderId: Long, content: String, id: Long = 0L)

    class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

      def senderId = column[Long]("sender")

      def content = column[String]("content")

      def * = (senderId, content, id) <> (Message.tupled, Message.unapply)

      def sender = foreignKey("sender_fk", senderId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
    }

    lazy val messages = TableQuery[MessageTable]
  }

  class Schema(val profile: scala.slick.driver.JdbcProfile) extends Tables with Profile

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter04", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>

      (messages.ddl ++ users.ddl).create

      messages.ddl.createStatements.foreach(println)

      // Users:
      val daveId: Long = insertUser += User("Dave")
      val halId: Long = insertUser += User("HAL")

      // Insert the conversation:
      messages ++= Seq(
        Message(daveId, "Hello, HAL. Do you read me, HAL?"),
        Message(halId, "Affirmative, Dave. I read you."),
        Message(daveId, "Open the pod bay doors, HAL."),
        Message(halId, "I'm sorry, Dave. I'm afraid I can't do that.")
      )

      // A simple join using the foreign key:
      val q = for {
        msg <- messages
        usr <- msg.sender
      } yield (usr.name, msg.content)

      println("Result of join: " + q.run)

      // Cascade delete
      users.filter(_.name === "Dave").delete

      println("Result of join: " + q.run)
  }
}