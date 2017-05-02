package chapter04.representations.tablescolumns.primarykeys

import chapter04.framework.Profile

object PrimaryKeyOption extends App {

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
  }

  class Schema(val profile: scala.slick.driver.JdbcProfile) extends Tables with Profile

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter04", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>

      users.ddl.create

      // A few users:
      println(s"Inserted Dave with id ${insertUser += User(None, "Dave", Some("dave@example.org"))}")
      println(s"Inserted HAL with id ${insertUser += User(None, "HAL")}")
      println(s"Inserted Elena with id ${insertUser += User(None, "Elena", Some("elena@example.org"))}")

      println(users.list)
  }
}