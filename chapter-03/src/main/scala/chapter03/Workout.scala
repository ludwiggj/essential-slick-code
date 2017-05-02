package chapter03

import scala.slick.driver.H2Driver.simple._

object Workout extends App {

  // Row representation

  // The id field at the end of the case class has a default value of 0L
  // This allows it to be omitted when creating a new object, without
  // having to pass the remaining arguments using keyword parameters.
  final case class Message(sender: String, content: String, id: Long = 0L)

  // Schema:
  final class MessageTable(tag: Tag) extends Table[Message](tag, "message") {

    // The id field of Message is mapped to an auto-incrementing primary key (using
    // the O.AutoInc option). The id value of the id field is ignored when
    // generating an insert query. The database steps in and generates the value.
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

      // Inserting data...

      // insert into "message" ("sender","content")  values (?,?)
      println(s"Basic insert: ${messages.insertStatement}")

      // Insert message
      val birthdayMsg = Message("Dave", "When's your birthday?")
      println(s"Adding message $birthdayMsg")
      val noOfRowsAdded = messages += birthdayMsg

      println(s"Added $noOfRowsAdded row")

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

      // Inserting specific columns...

      // Map over messages to insert a specific column
      println(s"SQL to insert message sender: ${messages.map(_.sender).insertStatement}")

      // This will generate a sql exception as we're not setting the content
      //      messages.map(_.sender) += "HAL"

      // Add multiple messages
      val testMessages = Seq(
        Message("Dave", "Hi HAL. Are you feeling better now?"),
        Message("HAL", "Not really, Dave.")
      )

      println(s"Inserted multiple messages...${messagesInsertWithId ++= testMessages}")

      displayMessages

      val rowsAdded: Option[Int] = messages ++= Seq(
        Message("Bob", "Gizza job?"),
        Message("Harry", "What?")
      )

      println(s"$rowsAdded rows added")

      displayMessages

      // Updating the database...

      val updateQuery = messages.filter(_.sender === "HAL").map(_.sender)

      // Update HAL's name:
      println(s"Updated ${updateQuery.update("HAL 9000")} senders from HAL to HAL 9000")

      println(s"SQL query: ${updateQuery.updateStatement}")

      displayMessages

      // Update HAL's name and message, by mapping message to a tuple:
      val query = messages.filter(_.id === 4L).map(message => (message.sender, message.content))

      println(s"Updated ${query.update("HAL 9000", "Sure, Dave. Come right in.")} row, id 4")
      println(s"via query: ${query.updateStatement}")

      displayMessages

      // Add an exclamation to the end of every message

      // This is not currently supported by update in Slick, but there are ways to achieve the same result.
      // One such way is to use plain SQL queries, which we cover in Chapter 6.
      // Another is to perform a client side update by defining a Scala function to capture the change to each row:
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

      // Deleting messages

      // Delete messages from HAL:
      // NB: will be zero rows affected because we've renamed HAL to HAL 9000
      val deleteQuery = messages.filter(_.sender === "HAL")

      println(s"Deleted ${deleteQuery.delete} rows")
      println(s"via query: ${deleteQuery.deleteStatement}")

      displayMessages

      val anotherDeleteQuery = messages.filter(_.sender === "HAL 9000")

      println(s"Deleted ${anotherDeleteQuery.delete} rows")
      println(s"via query: ${anotherDeleteQuery.deleteStatement}")

      displayMessages

      // It is an error to use delete in combination with map.

      // Compilation error:

      // value delete is not a member of scala.slick.lifted.Query[scala.slick.lifted.Column[String],String,Seq]
      // messages.map(_.content).delete

      // Update with a transaction
      def updateContent(id: Long) =
        messages.filter(_.id === id).map(_.content)

      println("Update 3 rows in a transaction")
      session.withTransaction {
        updateContent(1L).update("Wanna come in?")
        updateContent(3L).update("Pretty please!")
        updateContent(5L).update("Opening now.")
      }

      displayMessages

      // Roll back the changes

      session.withTransaction {
        updateContent(1L).update("Yo... Wanna come in?")
        updateContent(3L).update("Yo... Pretty please!")
        updateContent(5L).update("Yo... Opening now.")
        session.rollback
      }

      displayMessages

      // The rollback doesn’t happen until the withTransaction block ends. If we run queries within the
      // block, before the rollback actually occurs, they will still see the modified state:

      session.withTransaction {
        session.rollback
        updateContent(1L).update("Yo... Wanna come in?")
        messages.run
      }

      // Now rolled back
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