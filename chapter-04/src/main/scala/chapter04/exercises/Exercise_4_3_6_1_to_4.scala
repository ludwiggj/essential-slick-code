package chapter04.exercises

import chapter04.framework.Profile

object Exercise_4_3_6_1_to_4 extends App {

  trait Tables {
    this: Profile =>

    import profile.simple._

    case class User(id: Option[Long], name: String, email: Option[String] = None)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

      def name = column[String]("name")

      def email = column[Option[String]]("email")

      def * = (id.?, name, email) <> (User.tupled, User.unapply)
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

      // A few users:
      val daveId: Long = insertUser += User(None, "Dave", Some("dave@example.org"))
      val halId: Long = insertUser += User(None, "HAL")

      // Sometimes you want to look at all the users in the database, and sometimes you want to
      // only see rows matching a particular value. Working with the op􀦞onal email address for
      // a user, write a method that will take an optional value, and list rows matching that
      // value.
      def exercise_4_3_6_1() {
        def filterByEmail(email: Option[String]): Query[UserTable, UserTable#TableElementType, Seq] = {
          email match {
            case Some(email_address) => users.filter(_.email === email_address)
            case _ => users
          }
        }

        def filterByEmail2(email: Option[String]): Query[UserTable, UserTable#TableElementType, Seq] = {
          if (email.isEmpty) users
          else users.filter(_.email === email)
        }

        println("Find Dave")
        filterByEmail(Some("dave@example.org")).run.foreach(println)
        filterByEmail2(Some("dave@example.org")).run.foreach(println)

        println("Find all")
        filterByEmail(None).run.foreach(println)
        filterByEmail2(None).run.foreach(println)
      }

      // Build on the last exercise to match rows that start with the supplied optional value.
      def exercise_4_3_6_2() {
        def filterByEmailStartWith(email: Option[String]): Query[UserTable, UserTable#TableElementType, Seq] = {
          if (email.isEmpty) users
          else users.filter(_.email.startsWith(email.get))
        }

        def filterByEmailStartWith2(email: Option[String]): Query[UserTable, UserTable#TableElementType, Seq] = {
          email.map(e =>
            users.filter(_.email.startsWith(e))
          ) getOrElse users
        }

        println("Find Dave (dave@)")
        filterByEmailStartWith(Some("dave@")).run.foreach(println)
        filterByEmailStartWith2(Some("dave@")).run.foreach(println)

        println("Find Dave (davey@)")
        filterByEmailStartWith(Some("davey@")).run.foreach(println)
        filterByEmailStartWith2(Some("davey@")).run.foreach(println)
      }

      // Not everyone has an email address, so perhaps when filtering it would be safer to only
      // exclude rows that don’t match our filter criteria.
      def exercise_4_3_6_3() {
        val elenaId: Long = insertUser += User(None, "Elena", Some("elena@example.org"))

        def filterByEmail(email: Option[String]): Query[UserTable, UserTable#TableElementType, Seq] = {
          users.filter(u => u.email.isEmpty || u.email === email)
        }

        println("Find Dave (dave@)")
        filterByEmail(Some("dave@example.org")).run.foreach(println)
      }

      // What happens if you try adding a message for a user ID of 3000?
      // Note that there is no user in our example with an ID of 3000.
      def exercise_4_3_6_4() {

        // Exception in thread "main" org.h2.jdbc.JdbcSQLException: Referential integrity constraint
        // violation: "sender_fk: PUBLIC.""message"" FOREIGN KEY(""sender"") REFERENCES
        // PUBLIC.""user""(""id"") (3000)"; SQL statement:
        messages += Message(3000L, "Hello HAL!")
      }

      exercise_4_3_6_1()
      exercise_4_3_6_2()
      exercise_4_3_6_3()
      exercise_4_3_6_4()
  }
}