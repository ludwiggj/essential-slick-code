package chapter02

// Import the Slick interface for H2:
import scala.slick.driver.H2Driver.simple._
import scala.slick.lifted.Query

object Workout extends App {

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

  // An example query that selects a subset of messages:
  val halSays = messages.filter(_.sender === "HAL")

  // The types in the above filter expression deserve an explanation.

  // Slick represents all queries using a trait Query[M, U, C] that has three type parameters:

  //   M is called the mixed type. This is the function parameter type we see when calling methods like map and filter.
  //   U is called the unpacked type. This is the type we collect in our results.
  //   C is called the collection type. This is the type of collection we accumulate results into.

  // For example, a query that selects a subset of messages:
  private val messageTableToColumn: MessageTable => Column[Boolean] = (m: MessageTable) => m.sender === "HAL"

  // An example filter based on this criteria:
  //
  //        mixed type: MessageTable
  //     unpacked type: Message
  //   collection type: Seq
  val halExplicitlyTypedFilter: Query[MessageTable, Message, Seq] = messages.filter(messageTableToColumn)

  // The map operator changes both the mixed type and the unpacked type of the query.
  // The mixed type of the following query has changed to Column[String].
  val messageContent: Query[Column[String], String, Seq] = messages.map(_.content)

  // This means we are only passed the content column if we filter or map over this query:
  val seekBeauty1 = messageContent.filter { content: Column[String] => content like "%Pretty%" }
  val seekBeauty2 = messageContent.filter(_ like "%Pretty%")

  // The change of mixed type can complicate query composition with map. Therefore should only call map as the
  // final step in a sequence of transformations on a query, after all other operations have been applied.

  // Map to multiple columns: again, the mixed and unpacked types change accordingly
  val messagesTuples: Query[(Column[Long], Column[String]), (Long, String), Seq] = messages.map(t => (t.id, t.content))

  // We can also select column expressions as well as single Columns:
  val messagesIds: Query[Column[Long], Long, Seq] = messages.map(t => t.id * 1000L)

  val messageSenders: Query[Column[String], String, Seq] = messages.map(_.sender)

  def displaySql(sql: String) {
    println(s"\nSQL: ${sql}\n")
  }

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
    println("Inserting test data")
    messages ++= freshTestData

    println(s"All messages")
    println(messages.run)
    displaySql(messages.selectStatement)

    println("Selecting only messages from HAL:")
    halSays.run.foreach(println)
    displaySql(halSays.selectStatement)

    println("Selecting only messages from HAL (explicitly typed filter):")
    halExplicitlyTypedFilter.run.foreach(println)
    displaySql(halExplicitlyTypedFilter.selectStatement)

    println(s"All message content")
    println(messageContent.run)
    displaySql(messageContent.selectStatement)

    println("Selecting only Pretty messages:")
    println(seekBeauty1.run)
    displaySql(seekBeauty1.selectStatement)

    println(seekBeauty2.run)
    displaySql(seekBeauty2.selectStatement)

    println("Selecting all message tuples")
    println(messagesTuples.run)
    displaySql(messagesTuples.selectStatement)

    println("Selecting all message ids multiplied up")
    println(messagesIds.run)
    displaySql(messagesIds.selectStatement)

    println("Selecting all message sender names")
    messageSenders.run.foreach(println)
    displaySql(messageSenders.selectStatement)

    // Other invokers

    // Run returns the query’s collection type (Seq):
    println(s"All messages (Seq)")
    println(messages.run)

    // List always returns a List of the query’s unpacked type i.e. List[Message]:
    println("\nSelecting all messages as a list")
    println(messages.list)

    println("\nfirstOption returns first message (Option[Message])")
    println(messages.firstOption)

    println("\nFirst message sent by Dave")
    val daveMessages = messages.filter(_.sender === "Dave")
    println(daveMessages.firstOption)
    displaySql(daveMessages.selectStatement)

    println("\nFirst message sent by Nobody (None)")
    println(messages.filter(_.sender === "Nobody").firstOption)

    // If we want to retrieve large numbers of records, we can use the iterator invoker to return an Iterator of
    // results. We can extract results from the iterator one-at-a-time without consuming large amounts of memory:
    println("\nAll messages via iterator")
    messages.iterator.foreach(println)

    // Filter based on criteria with type Column[Boolean]
    def filterMessages(criteria: Workout.MessageTable => Column[Boolean]) {
      val query = messages.filter(criteria)
      println(query.list)
      displaySql(query.selectStatement)
    }

    // Experiments with filter to show different column expressions
    println("\nMessages sent by Dave (Type of m.sender === 'Dave' is Column[Boolean])")
    filterMessages((m: MessageTable) => m.sender === "Dave")

    println("\nMessages not sent by Dave (Type of m.sender =!= 'Dave' is Column[Boolean])")
    filterMessages((m: MessageTable) => m.sender =!= "Dave")

    println("\nMessages sent by someone alphabetically before HAL (Type of m.sender < 'HAL' is Column[Boolean])")
    filterMessages((m: MessageTable) => m.sender < "HAL")

    println("\nMessages sent by someone starting with D (Type of m.sender.startsWith('D') is Column[Boolean])")
    filterMessages((m: MessageTable) => m.sender.startsWith("D"))

    println("\nMessages sent by someone alphabetically later than message content (Type of m.sender >= m.content is Column[Boolean])")
    filterMessages((m: MessageTable) => m.sender >= m.content)

    // String methods
    // Slick provides the ++ method for string concatenation, similar to SQL ||
    println("\nMessages, sender and content:")
    val messagesSenderAndContent = messages.map(m => m.sender ++ "> " ++ m.content)

    messagesSenderAndContent.list.foreach(println)
    displaySql(messagesSenderAndContent.selectStatement)

    println("\nMessages with word 'Pretty' in message body")
    filterMessages((m: MessageTable) => m.content like "%Pretty%")

    println("\nMessages with odd id's larger than 1 (and'ing two Column[Boolean]'s together)")
    filterMessages((m: MessageTable) => (m.id > 2L) && (m.id % 2L =!= 0L))

    // Option Methods and Type Equivalence

    // can’t compare a String and an Int:
    // filterMessages((m: MessageTable) => m.id === "foo")

    // can’t compare an Int to a Long:
    // messages.filter((m: MessageTable) => m.id === 123)

    // Slick is clever about the equivalence of Optional and non-Optional columns.
    // As long as the operands are some combination of the types A and Option[A]
    // (for the same value of A), the query will normally compile:

    val messagesWithSpecificId = messages.filter(_.id === Option(4L))
    println("\nMessages with id 4:")
    messagesWithSpecificId.list.foreach(println)
    displaySql(messagesWithSpecificId.selectStatement)

    // However, any Optional arguments must be strictly of type Option, not Some or None:
    // messages.filter(_.id === Some(123L)).selectStatement

    // Sorting, take and drop

    println("\nMessages sorted on sender")
    val sortBySender = messages.sortBy(_.sender)
    sortBySender.list.foreach(println)
    displaySql(sortBySender.selectStatement)

    println("\nMessages sorted on content and then sender")
    val messagesSortedOnContentAndThenSender = messages.sortBy(m => (m.content, m.sender))
    messagesSortedOnContentAndThenSender.list.foreach(println)
    displaySql(messagesSortedOnContentAndThenSender.selectStatement)

    println("\nFirst two messages sorted on sender")
    val sortBySenderFirstTwo = sortBySender.take(2)
    sortBySenderFirstTwo.list.foreach(println)
    displaySql(sortBySenderFirstTwo.selectStatement)

    println("\nThird message sorted on sender")

    val sortBySenderThirdTake1 = sortBySender.take(1)
    sortBySenderThirdTake1.list.foreach(println)
    displaySql(sortBySenderThirdTake1.selectStatement)

    val sortBySenderThirdTake2 = messages.sortBy(_.sender).drop(2).take(1)
    sortBySenderThirdTake2.list.foreach(println)
    displaySql(sortBySenderThirdTake2.selectStatement)
  }

  // Note that the Iterator can only retrieve results while the session is open
  // foreach give error
  // org.h2.jdbc.JdbcSQLException: The object is already closed [90007-185]
  // as it's outside of withSession, so there is no implicit session

  //  db.withSession { implicit session =>
  //    messages.iterator
  //  }.foreach(println)
}