package chapter03.exercises

import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import chapter03.Helper.{messages, recreateDb}
import chapter03.Message
import scala.concurrent.ExecutionContext.Implicits.global

object Questions extends App {

  // Database connection details:
  val db = Database.forConfig("chapter03")

  // Helper method for running a query in this example file:
  def exec[T](program: DBIO[T]): T =
    Await.result(db.run(program), 5000 milliseconds)

  // Utility to print out what is in the database:
  def printCurrentDatabaseState() = {
    println("\nState of the database:\n")
    exec(messages.result.map(_.foreach(println)))
    println
  }

  try {
    println(s"${exec(recreateDb)} rows seeded into database")

    printCurrentDatabaseState()

    // Q 3.5.1 - insert specific columns
    println("Q 3.5.1 - insert specific columns")

    exec(messages.map(m => (m.sender, m.content)) += (("HAL", "Hi Honey I'm home!")))

    printCurrentDatabaseState()

    exec(recreateDb)

    // Q 3.5.2 - bulk all of the inserts

    // Return the messages with ids
    println("Q 3.5.2 - bulk all of the inserts")

    val messagesReturningRow = messages returning messages.map(_.id) into { (message, id) =>
      message.copy(id = id)
    }

    val conversation = List(
      Message("Bob", "Hi Alice"),
      Message("Alice", "Hi Bob"),
      Message("Bob", "Are you sure this is secure?"),
      Message("Alice", "Totally, why do you ask?"),
      Message("Bob", "Oh, nothing, just wondering."),
      Message("Alice", "Ten was too many messages"),
      Message("Bob", "I could do with a sleep"),
      Message("Alice", "Let's just to to the point"),
      Message("Bob", "Okay okay, no need to be tetchy."),
      Message("Alice", "Humph!")
    )

    exec(messagesReturningRow ++= conversation) foreach {
      println
    }

    printCurrentDatabaseState()

    exec(recreateDb)

    // Q 3.5.3 - query to delete messages that contain "sorry"
    println("Query to delete messages that contain 'sorry'.")

    exec(messages.filter(_.content like "%sorry%").delete)

    printCurrentDatabaseState()

    exec(recreateDb)

    // Q 3.5.4 - update two ways
    println("Q 3.5.4 - update two ways")

    println("Q 3.5.4 - first via methods")

    val rebootLoop = messages.filter(_.sender === "HAL").map(msg => (msg.sender, msg.content))
      .update(("HAL 9000", "Rebooting, please wait..."))
    exec(rebootLoop)

    printCurrentDatabaseState()

    exec(recreateDb)

    println("Q 3.5.4 - now via for comprehension")

    val halsMessages = for {
      message <- messages if message.sender === "HAL"
    } yield (message.sender, message.content)

    exec(halsMessages.update(("HAL 9000", "Rebooting, please wait...")))

    printCurrentDatabaseState()

    exec(recreateDb)

    // Q 3.5.5 - Delete HALs first two messages.

    // You don’t know the IDs of the messages, or the content of them. But you do know the IDs increase.
    // Hints:
    //   First write a query to select the two messages. Then see if you can find a way to use it as a subquery.
    //   You can use in in a query to see if a value is in a set of values returned from a query.

    println("Q 3.5.5 - Delete HALs first two messages")

    // Add a third message
    exec(messages.map(m => (m.sender, m.content)) += (("HAL", "Hi Honey I'm home!")))

    printCurrentDatabaseState()

    val first2HalMessages = messages.filter(_.sender === "HAL").sortBy(_.id.asc).take(2).map(_.id)

    val first2HalMessagesForDelete = messages.filter(_.id in first2HalMessages).delete

    println(s"SQL: ${first2HalMessagesForDelete.statements.head}")
    exec(first2HalMessagesForDelete)

    printCurrentDatabaseState()

  } finally db.close
}