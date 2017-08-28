package chapter05.framework

import chapter05.Message
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait DatabaseModule2 {
  // Self-type indicating that our tables must be mixed in with a Profile
  this: Profile =>

  // Whatever that Profile is, we import it as normal:
  import profile.api._

  final class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def sender = column[String]("sender")

    def content = column[String]("content")

    def * = (sender, content, id).mapTo[Message]
  }

  object messages extends TableQuery(new MessageTable(_)) {
    def messagesFrom(name: String) = this.filter(_.sender === name)

    val numSenders = this.map(_.sender).distinct.length
  }

}

object DbAbstractionFull extends App {

  // Bring all the components together:
  class DatabaseLayer(val profile: JdbcProfile) extends DatabaseModule2 with Profile

  // A specific driver
  val databaseLayer = new DatabaseLayer(slick.jdbc.H2Profile)

  // Use the schema:
  import databaseLayer._
  import profile.api._

  val db = Database.forConfig("chapter05")

  // Insert the conversation
  val msgs = Seq(
    chapter05.Message("Dave", "Hello, HAL. Do you read me, HAL?"),
    chapter05.Message("HAL", "Affirmative, Dave. I read you."),
    chapter05.Message("Dave", "Open the pod bay doors, HAL."),
    chapter05.Message("HAL", "I'm sorry, Dave. I'm afraid I can't do that.")
  )

  val program = for {
    _ <- messages.schema.create
    _ <- messages ++= msgs
    c <- messages.numSenders.result
  } yield c

  val result = Await.result(db.run(program), 2 seconds)
  println(s"Number of senders $result")
}