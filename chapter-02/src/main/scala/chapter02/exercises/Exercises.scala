package chapter02.exercises

// Import the Slick interface for H2:
import scala.slick.driver.H2Driver.simple._

import chapter02.Workout.MessageTable

object Exercises extends App {
  def displaySql(sql: String) {
    println(s"\nSQL: ${sql}\n")
  }

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
    messages ++= chapter02.Workout.freshTestData

    println("\nExercise 2.9.1: count messages")
    println(messages.list.length)

    println("\nExercise 2.9.2: select message with for comprehension")
    val query1 = (for {
      message <- messages if message.id === 1L
    } yield message)
    query1.list.foreach(println)
    displaySql(query1.selectStatement)

    // id 999L not present... no results printed
    (for {
      message <- messages if message.id === 999L
    } yield message).list.foreach(println)

    println("\nExercise 2.9.3: select message without comprehension")
    val query2 = messages.filter(_.id === 1L)
    query2.list.foreach(println)

    println("\nExercise 2.9.3.1: here's the sql")
    displaySql(query2.selectStatement)

    println("\nExercise 2.9.4: select message contents")
    val query3 = messages.map(_.content)
    query3.list.foreach(println)
    displaySql(query3.selectStatement)

    println("\nExercise 2.9.5: first message sent")
    val query4 = messages.filter(_.sender === "HAL").map(_.content)
    println(query4.firstOption)
    println(query4.first)

    val query5 = messages.filter(_.sender === "Alice")
    println(query5.firstOption)
    // Calling first will throw an exception
    // println(query5.first)

    println("\nExercise 2.9.6: Find the message that starts with 'Open'.")
    val query6 = messages.map(_.content).filter(_.startsWith("Open"))
    println(query6.list)
    displaySql(query6.selectStatement)

    println("\nExercise 2.9.7: Find all the messages with 'do' in their content")
    val query7 = messages.map(_.content).filter(_.like("%do%"))
    println(query7.list)
    displaySql(query7.selectStatement)

    println("\nExercise 2.9.7.1: Find all the messages with 'do' in their content, case insensitive")
    val query8 = messages.map(_.content).filter(_.toLowerCase.like("%do%"))
    println(query8.list)
    displaySql(query8.selectStatement)

    println("\nWhat does this do?")
    println(messages.map(_.content + "!").list)

    // The result is:

    // List(
    //   (message Path @461129530).content!,
    //   (message Path @461129530).content!,
    //   (message Path @461129530).content!,
    //   (message Path @461129530).content!,
    //   (message Path @461129530).content!
    // )

    // That is, a select expression for a strange constant string.
    // The _.content + "!" expression converts content to a string and appends the exclamation point.
    // content is a Column[String], not a String of the content. The end result is that we’re seeing
    // something of the internal workings of Slick.

    // It is possible to do this mapping in the database with Slick. We just need to remember to work
    // in terms of Column[T] classes:
    println("\nCorrected version, take 1")
    val query9 = messages.map(m => m.content ++ "!")
    println(query9.list)
    displaySql(query9.selectStatement)

    // LiteralColumn[T] is type of Column[T] for holding a constant value to be inserted into the SQL
    println("\nCorrected version, take 2")
    val query10 = messages.map(m => m.content ++ LiteralColumn("!"))
    println(query10.list)
    displaySql(query10.selectStatement)
  }
}