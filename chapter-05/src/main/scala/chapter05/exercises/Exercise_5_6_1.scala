package chapter05.exercises

import chapter05.framework.Profile
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Exercise_5_6_1 extends App {

  trait Tables {
    this: Profile =>

    import profile.api._

    // Users in the user table have an id which will
    // be used as a foreign key in the message table.
    case class User(name: String, email: Option[String], id: Long = 0L)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

      def name = column[String]("name")

      def email = column[Option[String]]("email")

      def * = (name, email, id).mapTo[User]
    }

    lazy val users = TableQuery[UserTable]

    def filterByEmail(email: Option[String]) = {
      email.isEmpty match {
        case false => users.filter(_.email === email)
        case true => users
      }
    }
  }

  class Schema(val profile: JdbcProfile) extends Tables with Profile

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  val db = Database.forConfig("chapter05")

  val initalise = DBIO.seq(
    users.schema.create,
    users += User("Dave", Some("dave@some.org")),
    users += User("HAL", None)
  )

  exec(initalise)

  println("\nUsers with email address dave@some.org:")
  exec(filterByEmail(Some("dave@some.org")).result).foreach {
    println
  }

  println("\nAll users:")
  exec(filterByEmail(None).result).foreach {
    println
  }
}