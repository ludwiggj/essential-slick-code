package chapter05.representations.tablescolumns.primarykeys

import chapter05.framework.Profile
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

// Code relating to 5.3.2 "Primary Keys".

object PrimaryKeyOption extends App {

  trait Tables {
    this: Profile =>

    import profile.api._

    // The names "HAL", "Dave" and so on, are now stored in a user table:
    case class User(id: Option[Long], name: String, email: Option[String] = None)

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

      def name = column[String]("name")

      def email = column[Option[String]]("email")

      def * = (id.?, name, email).mapTo[User]
    }

    lazy val users = TableQuery[UserTable]
    lazy val insertUser = users returning users.map(_.id)
  }

  class Schema(val profile: JdbcProfile) extends Tables with Profile

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  val db = Database.forConfig("chapter05")

  val init = for {
    _ <- users.schema.create
    _ <- insertUser ++= Seq(
      User(None, "Dave", Some("dave@example.org")),
      User(None, "HAL"),
      User(None, "Elena", Some("elena@example.org"))
    )
  } yield ()

  exec(init)

  println("\nUsers database contains:")
  exec(users.result).foreach {
    println
  }
}