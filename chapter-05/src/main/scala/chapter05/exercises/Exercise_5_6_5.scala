package chapter05.exercises

import chapter05.framework.Profile
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration._

object Exercise_5_6_5 extends App {

  trait Tables {
    this: Profile =>

    import profile.api._

    object UserRole extends Enumeration {
      type UserRole = Value
      val Owner = Value("O")
      val Regular = Value("R")
    }

    import UserRole._

    // Users in the user table have an id which will
    // be used as a foreign key in the message table.
    case class User(name: String, email: Option[String], role: UserRole = Regular, id: Long = 0L) {

    }

    // My version
    //    implicit val userRoleType =
    //      MappedColumnType.base[UserRole, Int](
    //        r => r match {
    //          case Regular => 1
    //          case Owner => 0
    //        },
    //        i => if (i == 0) Owner else Regular
    //      )

    // Textbook version
    implicit val userRoleType =
      MappedColumnType.base[UserRole, Int](
        _.id,
        id => UserRole.values.find(_.id == id) getOrElse Regular
      )

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

      def name = column[String]("name")

      def email = column[Option[String]]("email")

      def role = column[UserRole]("role")

      def * = (name, email, role, id).mapTo[User]
    }

    lazy val users = TableQuery[UserTable]
  }

  class Schema(val profile: JdbcProfile) extends Tables with Profile

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema.UserRole._
  import schema._
  import profile.api._

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  val db = Database.forConfig("chapter05")

  val initalise = DBIO.seq(
    users.schema.create,
    users += User("Dave", Some("dave@some.org"), Owner),
    users += User("HAL", None),
    users += User("Elena", Some("elena@example.org"))
  )

  exec(initalise)

  exec(users.result).foreach {
    println
  }
}