package chapter04.representations.tablescolumns

import chapter04.framework.Profile

object NullExample extends App {

  trait Tables {
    this: Profile =>

    import profile.simple._

    case class User(name: String, email: Option[String] = None, id: Long = 0L)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
      def name  = column[String]("name")
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

      users.ddl.createStatements.foreach(println)

      // Users:
      println(s"Inserted Dave with id ${insertUser += User("Dave", Some("dave@example.org"))}")
      println(s"Inserted HAL with id ${insertUser += User("HAL")}")
      println(s"Inserted Elena with id ${insertUser += User("Elena", Some("elena@example.org"))}")

      users.list.foreach(println)

      // Users with no email set
      users.filter(_.email.isEmpty).list.foreach(println)
  }
}