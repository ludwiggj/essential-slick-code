package chapter03

import scala.slick.driver.H2Driver.simple._

object Example extends App {

  // Row representation:
  final case class Message(sender: String, content: String, id: Long = 0L)

  // Schema:
  final class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def sender = column[String]("sender")

    def content = column[String]("content")

    def * = (sender, content, id) <> (Message.tupled, Message.unapply)
  }

  // Table:
  lazy val messages = TableQuery[MessageTable]

  // Database connection details:
  def db = Database.forURL("jdbc:h2:mem:chapter03", driver = "org.h2.Driver")

  def freshTestData = Seq(
    Message("Dave", "Hello, HAL. Do you read me, HAL?"),
    Message("HAL", "Affirmative, Dave. I read you."),
    Message("Dave", "Open the pod bay doors, HAL."),
    Message("HAL", "I'm sorry, Dave. I'm afraid I can't do that.")
  )

  // Query execution:
  db.withSession {
    implicit session =>

      // Create the table:
      messages.ddl.create

      // Add some data:
      messages ++= freshTestData

      displayMessages

      // See SQL for the insert statement
      println(s"Basic insert: ${messages.insertStatement}")

      // Insert message
      val birthdayMsg = Message("Dave", "When's your birthday?")
      println(s"Adding message $birthdayMsg")
      messages += birthdayMsg

      displayMessages

      // Insert message via insert statement
      val narkedMsg = Message("Dave", "You're off my Christmas card list.")
      println(s"Adding message $narkedMsg")
      messages insert narkedMsg

      displayMessages

      // Insert message with forceInsert, setting primary key explicitly
      println("Inserting message, setting id to 1000")
      messages forceInsert Message("Dave", "Point taken.", 1000)

      displayMessages

      val halIsBack = Message("HAL", "I'm back")
      // Insert message, returning the ID:
      val id = (messages returning messages.map(_.id)) += halIsBack
      println(s"The ID of inserted message $halIsBack is: $id")

      // Retrieve message by returned id
      println(s"The retrieved message is: ${messages.filter(_.id === id).firstOption}")

      displayMessages

      // Can create reusable query to retrieve id
      lazy val messagesInsert = messages returning messages.map(_.id)
      val jerkMessage = Message("Dave", "You're such a jerk.")
      val nextId = messagesInsert += jerkMessage
      println(s"The ID of inserted message $jerkMessage is: $nextId")

      displayMessages

      // Can only retrieve primary key from insert for H2
      // Following returns exception:
      // scala.slick.SlickException: This DBMS allows only a single AutoInc column to be returned from an INSERT

      // (messages returning messages) += Message("HAL", "I don't know. I guess we wait.")

      // If we do want to get a populated Message back from an insert for any database, we can do it by retrieving the
      // primary key and manually adding it to the inserted record. Slick simplifies this with another method, into:
      val messagesInsertWithId = messages returning messages.map(_.id) into { (message, id) =>
        message.copy(id = id)
      }

      val idiotMessage = Message("Dave", "You're an idiot as well.")
      println(s"Inserted message ${messagesInsertWithId += idiotMessage}")

      displayMessages

      // Map over messages to insert a specific column
      println(s"SQL to insert message sender: ${messages.map(_.sender).insertStatement}")

      // This will generate a sql exception as we're not setting the content
      //      messages.map(_.sender) += "HAL"

      // Add multiple messages
      val testMessages = Seq(
        Message("Dave", "Hi HAL. Are you feeling better now?"),
        Message("HAL", "Not really, Dave.")
      )

      val insertedMessages = messagesInsertWithId ++= testMessages
      println("Inserted multiple messages...")
      insertedMessages.foreach(println)

      displayMessages

      val updateQuery = messages.filter(_.sender === "HAL").map(_.sender)

      // Update HAL's name:
      println(s"Updated ${updateQuery.update("HAL 9000")} senders to HAL 9000")

      println(s"via query: ${updateQuery.updateStatement}")

      displayMessages

      // Update HAL's name and message, by mapping message to a tuple:
      val query = messages.filter(_.id === 4L).map(message => (message.sender, message.content))

      println(s"Updated ${query.update("HAL 9000", "Sure, Dave. Come right in.")} row, id 4")
      println(s"via query: ${query.updateStatement}")

      displayMessages

      // Add an exclamation to the end of every message
      def exclaim(msg: Message): Message = msg.copy(content = msg.content + "!")

      // We can update rows by selecting the relevant rows from the database, applying this function,
      // and writing the results back individually. Note that approach can be quite inefficient for
      // large datasets; it takes N + 1 queries to apply an update to N results

      println("Adding ! to end of every message's content")

      messages.iterator.foreach { message =>
        messages.filter(_.id === message.id).update(exclaim(message))
      }

      // Due to this inefficiency it's better to use plain SQL queries over this approach.

      displayMessages

      // Delete messages from HAL:
      // NB: will be zero rows affected because we've renamed HAL to HALL 9000
      val deleteQuery = messages.filter(_.sender === "HAL")

      println(s"Deleted ${deleteQuery.delete} rows")
      println(s"via query: ${deleteQuery.deleteStatement}")

      displayMessages

      // Update with a transaction
      def updateContent(id: Long) =
        messages.filter(_.id === id).map(_.content)

      println("Update 3 rows in a transaction")
      session.withTransaction {
        updateContent(2L).update("Wanna come in?")
        updateContent(3L).update("Pretty please!")
        updateContent(4L).update("Opening now.")
      }

      displayMessages
  }

  private def displayMessages = {
    db.withSession {
      implicit session =>
        // Current state of the database:
        println("\nState of the database:")
        messages.iterator.foreach(println)
        println
    }
  }
}