package chapter04.exercises

import chapter04.exercises.UserRole.UserRole
import chapter04.framework.Profile

// We can use the same trick that we’ve seen for DateTime and value classes to map enumerations.

// Here’s a Scala Enumeration for a user’s role:

// object UserRole extends Enumeration {
// type UserRole = Value
//   val Owner = Value("O")
//   val Regular = Value("R")
// }

// Modify the user table to include a UserRole. In the database store the role as a single character.

object UserRole extends Enumeration {
  type UserRole = Value
  val Owner = Value("O")
  val Regular = Value("R")
}

object Exercise_4_5_1_1_v1 extends App {

  trait Tables {
    this: Profile =>

    import profile.simple._

    implicit val charUserRoleType =
      MappedColumnType.base[UserRole, Char](
        ur => ur.toString.charAt(0),
        ch => ch match {
          case 'O' => UserRole.Owner
          case 'R' => UserRole.Regular
        }
      )

    case class User(id: Option[Long], name: String, role: UserRole, email: Option[String] = None)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

      def name = column[String]("name")

      def role = column[UserRole]("role")

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