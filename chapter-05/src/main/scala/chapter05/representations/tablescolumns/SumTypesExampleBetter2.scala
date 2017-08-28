package chapter05.representations.tablescolumns

import chapter05.framework.Profile
import chapter05.representations.tablescolumns.SumTypes.Tables
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object SumTypesBetter2 extends App {

  class Schema(val profile: JdbcProfile) extends Tables with Profile

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  // We can define some custom syntax to build our filter expressions:
  implicit class MessageQueryOps(message: MessageTable) {
    def isImportant = message.flag === (Important: Flag)

    def isOffensive = message.flag === (Offensive: Flag)

    def isSpam = message.flag === (Spam: Flag)
  }

  val db = Database.forConfig("chapter05")

  val program =
    for {
      _ <- ddl.create
      halId <- insertUser += User("HAL")
      daveId <- insertUser += User("Dave")
      count <- messages ++= Seq(
        Message(daveId, "Hello, HAL. Do you read me, HAL?"),
        Message(halId, "Affirmative, Dave. I read you."),
        Message(daveId, "Open the pod bay doors, HAL."),
        Message(halId, "I'm sorry, Dave. I'm afraid I can't do that.", Some(Important)))
      msgs <- messages.filter(_.isImportant).result
    } yield msgs

  val result = Await.result(db.run(program), 2 seconds)

  println("\nThe list of important messages:")
  println(result)
}