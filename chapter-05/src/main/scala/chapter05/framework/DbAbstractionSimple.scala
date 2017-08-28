package chapter05.framework

import chapter05.Message
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait DatabaseModule1 {
  val profile: JdbcProfile

  // Whatever that Profile is, we import it as normal
  import profile.api._

  // Write our db code here

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

object DbAbstractionSimple extends App {

  // Instantiate the database module, assigning a concrete profile:
  val databaseLayer = new DatabaseModule1 {
    val profile = slick.jdbc.H2Profile
  }

  import databaseLayer._
  import profile.api._

  val db = Database.forConfig("chapter05")

  // Insert the conversation
  val msgs = Seq(
    Message("Dave", "Hello, HAL. Do you read me, HAL?"),
    Message("HAL", "Affirmative, Dave. I read you."),
    Message("Dave", "Open the pod bay doors, HAL."),
    Message("HAL", "I'm sorry, Dave. I'm afraid I can't do that.")
  )

  val program = for {
    _ <- messages.schema.create
    _ <- messages ++= msgs
    c <- messages.numSenders.result
  } yield c

  val result = Await.result(db.run(program), 2 seconds)
  println(s"Number of senders $result")
}