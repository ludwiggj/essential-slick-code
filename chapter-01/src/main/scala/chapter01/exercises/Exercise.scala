package chapter01.exercises

// Import the Slick interface for H2:
import scala.slick.driver.H2Driver.simple._

import chapter01.Example.{Message, MessageTable}

object Exercise extends App {
  def db = Database.forURL(
    url = "jdbc:h2:mem:chat-database;DB_CLOSE_DELAY=-1",
    driver = "org.h2.Driver")

  // Base query for querying the messages table:
  val messages = TableQuery[MessageTable]

  val message = Message("Dave", "What if I say 'Pretty please'?")

  val daveMessages = messages.filter((m: MessageTable) => m.sender === "Dave")

  // Connect to the database...
  db.withSession { implicit session =>
    println("Creating database table")
    messages.ddl.create

    println("\nInserting test data")
    messages ++= chapter01.Example.freshTestData

    println("\nExercise 1: insert message")
    println(messages += message)

    println("\nExercise 2: retrieve Dave's messages")
    daveMessages.run.foreach(println(_))
  }
}