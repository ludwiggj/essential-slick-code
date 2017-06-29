package chapter01

// Import the Slick interface for H2:
import chapter01.Helper.freshTestData
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Workout extends App {
  // Base query for querying the messages table:
  lazy val messages = TableQuery[MessageTable]

  // NOTE: Rep is common base trait for all lifted values, including columns
  println(messages) // Rep(TableExpansion)

  // An example query that selects a subset of messages:
  val halSays: Query[MessageTable, Message, Seq] = messages.filter(_.sender === "HAL")

  println(halSays) // Rep(Filter @1014166943)

  // Create an in-memory H2 database;
  val db = Database.forConfig("chapter01")

  // Helper method for running a query in this example file:
  def exec[T](program: DBIO[T]): T = {
    val eventualT: Future[T] = db.run(program)
    val result = Await.result(eventualT, 2 seconds)
    println(s"Result: $result")
    result
  }

  // Create the "messages" table:
  println("Creating database table")
  println(s"Will run: ${messages.schema.createStatements.mkString}")
  private val createAction: DBIO[Unit] = messages.schema.create
  exec(createAction)

  // Create and insert the test data:
  println("\nInserting test data")
  private val insertAction: DBIO[Option[Int]] = messages ++= freshTestData
  exec(insertAction)

  // Run the test query and print the results:
  println("\nSelecting all messages:")
  println(s"Will run: ${messages.result.statements.mkString}")
  private val messagesAction: DBIO[Seq[Message]] = messages.result
  exec(messagesAction) foreach {
    println
  }

  println("\nSelecting only messages from HAL:")
  println(s"Will run: ${halSays.result.statements.mkString}")
  private val filteredMessagesAction: DBIO[Seq[Message]] = halSays.result
  exec(filteredMessagesAction) foreach {
    println
  }

  // Use map to select subset of columns
  println("\nUse map to select subset of columns:")
  private val filteredColumns: Query[Rep[Long], Long, Seq] = halSays.map(_.id)
  println(s"Will run: ${filteredColumns.result.statements.mkString}")
  private val filteredColumnsAction = filteredColumns.result
  exec(filteredColumnsAction) foreach {
    println
  }

  // For comprehension equivalent...
  val halSays2: Query[MessageTable, Message, Seq] = for {
    message <- messages if message.sender === "HAL"
  } yield message

  exec(halSays2.result)
}