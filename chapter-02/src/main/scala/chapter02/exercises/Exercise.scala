package chapter02.exercises

// Import the Slick interface for H2:
import chapter02.Example.{Message, MessageTable}

import scala.slick.driver.H2Driver.simple._

object Exercise extends App {
  def db = Database.forURL(
    url = "jdbc:h2:mem:chat-database;DB_CLOSE_DELAY=-1",
    driver = "org.h2.Driver")

  // Base query for querying the messages table:
  val messages = TableQuery[MessageTable]

  // Connect to the database...
  db.withSession { implicit session =>
    println("Creating database table")
    messages.ddl.create

    println("\nInserting test data")
    messages ++= chapter02.Example.freshTestData

    println("\nExercise 1: count messages")
    println(messages.list.length)

    println("\nExercise 2: select message with for comprehension")
    val query1 = (for {
      message <- messages if message.id === 1L
    } yield message)
    println(query1.selectStatement)
    query1.list.foreach(println)

    (for {
      message <- messages if message.id === 999L
    } yield message).list.foreach(println)

    println("\nExercise 3: select message without comprehension")
    val query2 = messages.filter(_.id === 1L)
    println(query2.selectStatement)
    query2.list.foreach(println)

    println("\nExercise 4: select message contents")
    val query3 = messages.map(_.content)
    println(query3.selectStatement)
    query3.list.foreach(println)

    println("\nExercise 5: first message sent")
    val query4 = messages.filter(_.sender === "HAL").map(_.content)
    println(query4.firstOption)
    println(query4.first)

    val query5 = messages.filter(_.sender === "Alice")
    println(query5.firstOption)

    println("\nExercise 6: Find the message that starts with 'Open'.")
    val query6 = messages.map(_.content).filter(_.startsWith("Open"))
    println(query6.selectStatement)
    println(query6.list)

    println("\nExercise 7: Find all the messages with 'do' in their content")
    val query7 = messages.map(_.content).filter(_.like("%do%"))
    println(query7.selectStatement)
    println(query7.list)

    println("\nExercise 8: Find all the messages with 'do' in their content, case insensitive")
    val query8 = messages.map(_.content).filter(_.toLowerCase.like("%do%"))
    println(query8.selectStatement)
    println(query8.list)

    println("\nWhat does this do?")
    println(messages.map(_.content + "!").list)

    println("\nCorrected version, take 1")
    val query9 = messages.map(m => m.content ++ "!")
    println(query9.selectStatement)
    println(query9.list)

    println("\nCorrected version, take 2")
    val query10 = messages.map(m => m.content ++ LiteralColumn("!"))
    println(query10.selectStatement)
    println(query10.list)
  }
}