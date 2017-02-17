package chapter02

// Import the Slick interface for H2:
import scala.slick.driver.H2Driver.simple._

// Import the Slick interface for H2:

object Example extends App {

  // Case class representing a row in our table:
  final case class Message(sender: String, content: String, id: Long = 0L)

  // Helper method for creating test data:
  def freshTestData = Seq(
    Message("Dave", "Hello, HAL. Do you read me, HAL?"),
    Message("HAL", "Affirmative, Dave. I read you."),
    Message("Dave", "Open the pod bay doors, HAL."),
    Message("HAL", "I'm sorry, Dave. I'm afraid I can't do that."),
    Message("Dave", "What if I say 'Pretty please'?")
  )

  // Schema for the "message" table:
  final class MessageTable(tag: Tag)
    extends Table[Message](tag, "message") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def sender = column[String]("sender")

    def content = column[String]("content")

    def * = (sender, content, id) <> (Message.tupled, Message.unapply)
  }

  // Base query for querying the messages table:
  lazy val messages = TableQuery[MessageTable]

  println(s"${messages.selectStatement}")

  // An example query that selects a subset of messages:
  val halSays = messages.filter(_.sender === "HAL")
  println(s"halSays: ${halSays.selectStatement}")

  val halSays2 = messages.filter { messageTable: MessageTable =>
    messageTable.sender === "HAL"
  }
  println(s"halSays2: ${halSays2.selectStatement}")

  // Create a permanent in-memory H2 database;
  def db = Database.forURL(
    url = "jdbc:h2:mem:chat-database;DB_CLOSE_DELAY=-1",
    driver = "org.h2.Driver")

  // Connect to the database...
  db.withSession { implicit session =>
    // Create the "messages" table:
    println("Creating database table")
    messages.ddl.create

    // Create and insert the test data:
    println("\nInserting test data")
    messages ++= freshTestData

    // Run the test query and print the results:
    println("\nSelecting all message sender names")
    val messageSenders: Query[Column[String], String, Seq] = messages.map(_.sender)
    messageSenders.run.foreach(println)

    println(s"All message sender names SQL: ${messageSenders.selectStatement}")

    println(s"All message content")
    val messageContent: Query[Column[String], String, Seq] = messages.map(_.content)
    println(messageContent.run)

    // Seems that filter call here must use {} rather than ()
    val seekBeauty1 = messageContent.filter { content: Column[String] => content like "%Pretty%" }
    val seekBeauty2 = messageContent.filter(_ like "%Pretty%")

    println("\nSelecting only Pretty messages:")
    println(seekBeauty1.run)
    println(seekBeauty2.run)

    println("\nSelecting only Pretty messages, SQL:")
    println(seekBeauty1.selectStatement)
    println(seekBeauty2.selectStatement)

    println("\nSelecting all message tuples")
    val messagesTuples: Query[(Column[Long], Column[String]), (Long, String), Seq] = messages.map(t => (t.id, t.content))
    println(messagesTuples.run)

    println(s"All message tuples SQL: ${messagesTuples.selectStatement}")

    // We can also select column expressions as well as single Columns:
    println("\nSelecting all message ids multiplied up")
    val messagesIds: Query[Column[Long], Long, Seq] = messages.map(t => t.id * 1000L)
    println(messagesIds.run)

    println(s"All message ids SQL: ${messagesIds.selectStatement}")

    // Other invokers
    println("\nSelecting all message tuples as a list")
    println(messagesTuples.list)

    println("\nFirst message")
    println(messages.firstOption)

    println("\nFirst message sent by Dave")
    println(messages.filter(_.sender === "Dave").firstOption)

    println("\nFirst message sent by Nobody")
    println(messages.filter(_.sender === "Nobody").firstOption)

    // If we want to retrieve large numbers of records, we can use the iterator invoker to return an Iterator of
    // results. We can extract results from the iterator one-at-a-􀦞me without consuming large amounts of memory:
    messages.iterator.foreach(println)

    def filterMessages(criteria: Example.MessageTable => Column[Boolean]) {
      val query = messages.filter(criteria)
      println(s"Running statement:  ${query.selectStatement}\ngiving result    :  ${query.list}")
    }

    // Experiments with filter to show different column expressions
    println("\nMessages sent by Dave")
    filterMessages((m: MessageTable) => m.sender === "Dave")

    println("\nMessages not sent by Dave")
    filterMessages((m: MessageTable) => m.sender =!= "Dave")

    println("\nMessages sent by someone alphabetically before HAL")
    filterMessages((m: MessageTable) => m.sender < "HAL")

    println("\nMessages sent by someone starting with D")
    filterMessages((m: MessageTable) => m.sender.startsWith("D"))

    println("\nMessages sent by someone alphabetically later than message content")
    filterMessages((m: MessageTable) => m.sender >= m.content)

    // String methods
    // Slick provides the ++ method for string concatenation
    val messagesSenderAndContent = messages.map(m => m.sender ++ "> " ++ m.content)
    println(s"\n${messagesSenderAndContent.selectStatement}")
    messagesSenderAndContent.list.foreach(println)

    println("\nMessages with word 'Pretty' in message body")
    filterMessages((m: MessageTable) => m.content like "%Pretty%")

    println("\nMessages with odd id's larger than 1")
    filterMessages((m: MessageTable) => (m.id > 2L) && (m.id % 2L =!= 0L))

    // Sorting

    println("\nMessages sorted on sender")
    messages.sortBy(_.sender).list.foreach(println)

    println("\nMessages sorted on sender and then content")
    val messagesSortedOnSenderAndThenContent = messages.sortBy(m => (m.sender, m.content))
    println(messagesSortedOnSenderAndThenContent.selectStatement)
    messagesSortedOnSenderAndThenContent.list.foreach(println)

    println("\nFirst two messages sorted on sender")
    messages.sortBy(_.sender).take(2).list.foreach(println)

    println("\nThird message sorted on sender")
    messages.sortBy(_.sender).drop(2).take(1).list.foreach(println)
  }

  // Note that the Iterator can only retrieve results while the session is open
  // foreach give error
  // org.h2.jdbc.JdbcSQLException: The object is already closed [90007-185]
  // as it's outside of withSession, so there is no implicit session

  //  db.withSession { implicit session =>
  //    messages.iterator
  //  }.foreach(println)
}