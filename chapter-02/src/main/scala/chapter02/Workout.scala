package chapter02

// Import the Slick interface for H2:
import chapter02.Helper.freshTestData
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration._

object Workout extends App {

  // Base query for querying the messages table:
  lazy val messages = TableQuery[MessageTable]

  // An example query that selects a subset of messages:
  val halSays = messages.filter { m: MessageTable => m.sender === "HAL" }

  // Create an in-memory H2 database;
  val db = Database.forConfig("chapter02")

  // Helper method for running a query in this example file:
  def exec[T](program: DBIO[T]): T = Await.result(db.run(program), 2.seconds)

  try {

    // Create the "messages" table:
    println("Creating database table andThen inserting test data")
    exec(messages.schema.create andThen (messages ++= freshTestData)) foreach {
      println
    }

    println("\nAll users:")
    exec(messages.result) foreach {
      println
    }

    println("\nMessage content:")
    println(messages.map(_.content).result.statements.mkString)
    exec(messages.map(_.content).result) foreach {
      println
    }

    // Run the test query and print the results:
    println("\nSelecting all message sender names:")
    exec(messages.map(_.sender).result) foreach {
      println
    }

    println("\nSelecting only Pretty messages:")
    println(exec {
      messages.map(_.content).filter { content: Rep[String] => content like "%Pretty%" }.result
    })

    println("\nSelecting two columns:")
    println(messages.map(t => (t.id, t.content)).result.statements.mkString)
    println(exec(messages.map(t => (t.id, t.content)).result))

    println("\nMap columns to a specific case class:")
    case class TextOnly(id: Long, content: String)
    val contentQuery = messages.map(t => (t.id, t.content).mapTo[TextOnly])
    exec(contentQuery.result) foreach {
      println
    }

    println("\nColumn expression example:")
    println(messages.map(t => t.id * 1000L).result.statements.mkString)
    exec(messages.map(t => t.id * 1000L).result) foreach {
      println
    }

    // Exists

    val containsBay = for {
      m <- messages
      if m.content like "%bay%"
    } yield m

    val bayMentioned: DBIO[Boolean] = containsBay.exists.result

    println(exec(bayMentioned))

    exec(containsBay.result) foreach { println }

  } finally db.close
}