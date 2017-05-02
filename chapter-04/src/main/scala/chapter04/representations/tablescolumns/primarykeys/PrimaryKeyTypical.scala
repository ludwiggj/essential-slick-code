package chapter04.representations.tablescolumns.primarykeys

import chapter04.framework.Profile

object PrimaryKeyTypical extends App {

  trait Tables {
    this: Profile =>

    import profile.simple._

    case class User(name: String, email: Option[String] = None, id: Long = 0L)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

      def name = column[String]("name")

      def email = column[Option[String]]("email")

      def * = (name, email, id) <> (User.tupled, User.unapply)
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
      val daveId: Long = insertUser += User("Dave", Some("dave@example.org"))
      val halId: Long = insertUser += User("HAL")
      val elena: Long = insertUser += User("Elena", Some("elena@example.org"))

      println(users.list)
  }
}