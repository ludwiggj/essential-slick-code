package chapter04.representations.rows.caseclasses

import chapter04.framework.Profile

import scala.slick.driver.H2Driver.simple._
import scala.slick.driver.JdbcProfile
import scala.slick.lifted.Tag

final case class User(name: String, id: Long = 0L)

/*
final class UserTableInvalid(tag: Tag) extends Table[User](tag, "user") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")

  // Following is invalid
  def * = (name, id)

  // Compiler has spotted that we’ve not defined a default projection to supply Users

  // Error message:

  // type mismatch;
  // found   : (scala.slick.lifted.Column[String], scala.slick.lifted.Column[Long])
  // required: scala.slick.lifted.ProvenShape[chapter04.representations.caseclasses.User]
}
*/

// Slick requires rules to prove it can convert from the Column values into the shape we want,
// which is a case class. This is the role of the mapping function, <>.

// The two arguments to <> are:

// (1) a function from U => R, which converts from our unpacked row-level encoding into our preferred representation
// (2) a function from R => Option[U], which is going the other way.

final class UserTableMappingByHand(tag: Tag) extends Table[User](tag, "user") {
  def name = column[String]("name")

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  private def intoUser(pair: (String, Long)): User = User(pair._1, pair._2)

  private def fromUser(user: User): Option[(String, Long)] = Some((user.name, user.id))

  def * = (name, id) <> (intoUser, fromUser)
}

trait Tables {
  // Self-type indicating that our tables must be mixed in with a Profile
  this: Profile =>

  // Whatever that Profile is, we import it as normal:
  import profile.simple._

  final class UserTable(tag: Tag) extends Table[User](tag, "user") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def * = (name, id) <> (User.tupled, User.unapply)
  }

  lazy val users = TableQuery[UserTable]
}

// Bring all the components together:
class Schema(val profile: JdbcProfile) extends Tables with Profile

object Main extends App {
  // A specific schema with a particular driver:
  val schema = new Schema(scala.slick.driver.H2Driver)

  // Use the schema:
  import schema._, profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter03", driver = "org.h2.Driver")

  db.withSession {
    // Work with the database as normal here

    implicit session =>

      users.ddl.create
      println(users.insertStatement)

      users += User("Dave")
      println(users.list)
  }
}