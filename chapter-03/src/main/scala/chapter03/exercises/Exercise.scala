package chapter03.exercises

// Import the Slick interface for H2:
import scala.slick.driver.H2Driver.simple._
import chapter03.Example.{Message, MessageTable}

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
    messages ++= chapter03.Example.freshTestData

    def displayMessages {
      println("\nMessages Table:")
      messages.list.foreach(println)
    }

    displayMessages

    println("\nExercise 1: Method to insert a message for someone, but only if the message content hasn’t already been"
      + "stored. We want the id of the message as a result.")

    def insertOnce(sender: String, message: String): Long = {
      println(s"Adding [$message], sent by [$sender]")
      val existingMsg = messages.filter(m => m.sender === sender && m.content === message).firstOption

      existingMsg match {
        case Some(m) => m.id
        case _ => (messages returning messages.map(_.id)) += Message(sender, message)
      }
    }

    def insertOnceImproved(sender: String, message: String): Long = {
      println(s"Adding [$message], sent by [$sender]")
      val query = messages.filter(m => m.sender === sender && m.content === message).map(_.id)

      query.firstOption getOrElse (messages returning messages.map(_.id) += Message(sender, message))
    }

    println(insertOnceImproved("Dave", "Hello, HAL. Do you read me, HAL?"))
    displayMessages

    println(insertOnceImproved("HAL", "Hello, HAL. Do you read me, HAL?"))
    displayMessages

    println(insertOnceImproved("Dave", "Watsup, HAL. Do you read me, HAL?"))
    displayMessages

    println("\nExercise 2: What is the state of the database after this code is run? Is 'Surprised?' printed?")
    session.withTransaction {
      messages.delete       // deletes messages
      displayMessages       // no messages
      session.rollback()    // rollback doesn't happen yet
      displayMessages       // still no messages
      messages.delete       // delete them again - though there aren't any!
      displayMessages       // still no messages
      println("Surprised?")
                            // Rollback only occurs on exiting block
    }

    displayMessages         // Now all the messages are back!

    println("\nExercise 3: Rewrite using a for comprehension")

    println("Query to update sender HAL to 'HAL 9000' updates "
      + s"${messages.filter(_.sender === "HAL").map(_.sender).update("HAL 9000")} rows")

    displayMessages

    // Now as a for comprehension
    val updateQuery = for {
      message <- messages if message.sender === "HAL 9000"
    } yield (message.sender)

    println(s"Query to update sender 'HAL 9000' to HAL updates ${updateQuery.update("HAL")} rows")

    displayMessages

    println("\nExercise 4: Delete all messages")

    println(s"Deleted all rows (${messages.delete} rows)")

    displayMessages
  }
}