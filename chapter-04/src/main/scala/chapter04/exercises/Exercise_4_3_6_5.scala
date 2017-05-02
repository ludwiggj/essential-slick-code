package chapter04.exercises

import chapter04.framework.Profile

// We’re now charging for our chat service. Outstanding payments will be stored in a table called bill. The
// default change is $12.00, and bills are recorded against a user. A user should only have one or zero entries in
// this table. Make sure it is impossible for a user to be deleted while they have a bill to pay.
// Go ahead and model this.

// Hint: Remember to include your new table when creating the schema:

// Additionally, provide queries to give the full details of users:
// • who do have an outstanding bill; and
// • who have no outstanding bills.
// Hint: Slick provides in for SQL’s WHERE x IN (SELECT ...) expressions

object Exercise_4_3_6_5 extends App {

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

    case class Bill(userId: Long, amount: BigDecimal)

    class BillTable(tag: Tag) extends Table[Bill](tag, "bill") {
      def userId = column[Long]("user", O.PrimaryKey)

      def amount = column[BigDecimal]("amount", O.Default(12.00))

      def * = (userId, amount) <> (Bill.tupled, Bill.unapply)

      def user = foreignKey("fk_bill_user", userId, users)(_.id, onDelete = ForeignKeyAction.Restrict)
    }

    lazy val bills = TableQuery[BillTable]
  }

  class Schema(val profile: scala.slick.driver.JdbcProfile) extends Tables with Profile

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter04", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>

      (bills.ddl ++ messages.ddl ++ users.ddl).create

      // A few users:
      val daveId: Long = insertUser += User(None, "Dave", Some("dave@example.org"))
      val bobId: Long = insertUser += User(None, "Bob", None)
      val carlyId: Long = insertUser += User(None, "Carly", Some("carly@example.org"))
      val elenaId: Long = insertUser += User(None, "Elena", None)

      bills += Bill(daveId, 100)

      // Exception in thread "main" org.h2.jdbc.JdbcSQLException: Unique index or primary key violation
      // bills += Bill(daveId, 100)

      bills += Bill(elenaId, 50.2)

      println(bills.list)

      // Can't delete from users...

      // JdbcSQLException: Referential integrity constraint violation: "user_fk: PUBLIC.""bill""
      // FOREIGN KEY(""user"") REFERENCES PUBLIC.""user""(""id"") (1)"; ...

      // users.delete

      // Additionally, provide queries to give the full details of users:
      // • who do have an outstanding bill; and
      // • who have no outstanding bills.
      // Hint: Slick provides in for SQL’s WHERE x IN (SELECT ...) expressions

      println("Users with outstanding bills")
      println(users.filter(_.id in bills.map(_.userId)).list)

      // Alternatively...

      val has = for {
        b <- bills
        u <- b.user
      } yield u

      println(has.list)

      println("Users with no outstanding bills")
      println(users.filterNot(_.id in bills.map(_.userId)).list)

      // Alternatively...

      val hasNot = for {
        u <- users
        if !(u.id in bills.map(_.userId))
      } yield u

      println(hasNot.list)
  }
}