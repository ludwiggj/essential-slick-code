package chapter04.exercises

import chapter04.exercises.UserRole.UserRole
import chapter04.framework.Profile

object Exercise_4_5_1_1_v2 extends App {

  trait Tables {
    this: Profile =>

    import profile.simple._

    implicit val stringUserRoleType =
      MappedColumnType.base[UserRole, String](
        _.toString, UserRole.withName(_)
      )

    case class User(id: Option[Long], name: String, role: UserRole, email: Option[String] = None)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

      def name = column[String]("name")

      def role = column[UserRole]("role", O.Length(1, false))

      def email = column[Option[String]]("email")

      def * = (id.?, name, role, email) <> (User.tupled, User.unapply)
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
      val daveId: Long = insertUser += User(None, "Dave", UserRole.Owner, Some("dave@example.org"))
      val bobId: Long = insertUser += User(None, "Bob", UserRole.Regular, None)

      println(users.list)
  }
}