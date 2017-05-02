package chapter04.representations.rows.tuples

import chapter04.framework.Profile
import AnotherUserTable.TupleUser

import scala.slick.driver.JdbcProfile

object AnotherUserTable {
  type TupleUser = (String, Long)
}

trait Tables {
  // Self-type indicating that our tables must be mixed in with a Profile
  this: Profile =>

  // Whatever that Profile is, we import it as normal:
  import profile.simple._

  final class AnotherUserTable(tag: Tag) extends Table[TupleUser](tag, "user") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def * = (name, id)
  }

  lazy val users = TableQuery[AnotherUserTable]
}

// Bring all the components together:
class Schema(val profile: JdbcProfile) extends Tables with Profile

object Main extends App {
  // A specific schema with a particular driver:
  val schema = new Schema(scala.slick.driver.H2Driver)

  // Use the schema:
  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter03", driver = "org.h2.Driver")

  db.withSession {
    // Work with the database as normal here

    implicit session =>

      users.ddl.create

      users forceInsert (("Puffin", 3L))

      println(users.list)
  }
}