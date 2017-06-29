package chapter01

// Import the Slick interface for H2:
import chapter01.Helper.freshTestData
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object ActionCombiner extends App {

  // Base query for querying the messages table:
  lazy val messages = TableQuery[MessageTable]

  // Create an in-memory H2 database;
  val db = Database.forConfig("chapter01")

  // Helper method for running a query in this example file:
  def exec[T](program: DBIO[T]): T = {
    val eventualT: Future[T] = db.run(program)
    val result = Await.result(eventualT, 2 seconds)
    println(s"Result: $result")
    result
  }

  val halSays: Query[MessageTable, Message, Seq] = messages.filter(_.sender === "HAL")

  private val createAction: DBIO[Unit] = messages.schema.create
  private val insertAction: DBIO[Option[Int]] = messages ++= freshTestData
  private val filteredMessagesAction: DBIO[Seq[Message]] = halSays.result

  val actions: DBIO[Seq[Message]] = createAction andThen insertAction andThen filteredMessagesAction

  exec(actions)
}