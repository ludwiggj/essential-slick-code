package chapter04.representations.tablescolumns

import chapter04.framework.Profile

object ModifiersExample extends App {

  trait Tables {
    this: Profile =>

    import profile.simple._

    case class User(name: String, country: Option[String] = None, avatar: Option[Array[Byte]] = None, id: Long = 0L)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

      // name has maximum length, and a default value
      def name = column[String]("name", O.Length(64, true), O.Default("Anonymous Coward"))

      def country = column[Option[String]]("country", O.Default(Some("UK")))

      // DBType controls the exact type used by the database
      def avatar = column[Option[Array[Byte]]]("avatar", O.DBType("BINARY(2048)"))

      // default composite index
      def nameIndex = index("name_idx", (name, avatar), unique = true)

      def * = (name, country, avatar, id) <> (User.tupled, User.unapply)
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

      // Users:
      val halId: Long = insertUser += User("HAL")
      val daveId: Long = insertUser += User("Dave")
      val fracoisId: Long = insertUser += User("Francois", Some("France"))

      println(users.list)
  }
}