package chapter02.exercises

import chapter02.Helper.freshTestData
import chapter02.MessageTable
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration._

object Questions extends App {
  // Create an in-memory H2 database;
  val db = Database.forConfig("chapter02")

  // Helper method for running a query in this example file:
  def exec[T](program: DBIO[T]): T = Await.result(db.run(program), 2.seconds)

  lazy val messages = TableQuery[MessageTable]

  // Create the "messages" table:
  println("Creating database table")
  println(s"Will run: ${messages.schema.createStatements.mkString}")
  private val createAction: DBIO[Unit] = messages.schema.create
  exec(createAction)

  // Create and insert the test data:
  println("\nInserting test data")
  private val insertAction: DBIO[Option[Int]] = messages ++= freshTestData
  exec(insertAction)

  // Q_2.10.1
  println("Q_2.10.1:")

  println(s"No of messages (1): ${exec(messages.result).length}")
  println(s"No of messages (2): ${exec(messages.length.result)}")

  // Q_2.10.2
  println("Q_2.10.2:")

  val messageId1 = for {
    message <- messages if message.id === 1L
  } yield message

  println(s"Message with ID 1: ${exec(messageId1.result)}")

  val messageId99 = for {
    message <- messages if message.id === 99L
  } yield message

  println(s"Message with ID 99: ${exec(messageId99.result)}")

  // Q_2.10.3
  println("Q_2.10.3:")

  val messageId1Take2 = messages.filter(_.id === 1L)
  println(s"Message with ID 1: ${exec(messageId1Take2.result)}")

  // Q_2.10.4
  println("Q_2.10.4:")

  println(s"SQL for message with ID 1...")
  messageId1Take2.result.statements foreach {
    println
  }

  // Q_2.10.5
  println("Q_2.10.5:")

  val anyMessagesFromHal = messages.filter(_.sender === "HAL").exists
  println(s"Any messages from HAL? ${exec(anyMessagesFromHal.result)}")

  // Q_2.10.6
  println("Q_2.10.6:")

  println("Message content...")
  val messageContent = messages.map(_.content)
  println(s"SQL: ${messageContent.result.statements.head}")
  exec(messageContent.result) foreach {
    println
  }

  // Q_2.10.7
  println("Q_2.10.7:")

  println("First message from HAL:")
  val messagesFromHal = messages.filter(_.sender === "HAL").map(_.content)
  println(exec(messagesFromHal.result.head))

  println("First message from Alice:")
  val messagesFromAlice = messages.filter(_.sender === "Alice").map(_.content)

  // Error!
  // println(exec(messagesFromAlice.result.head))
  println(exec(messagesFromAlice.result.headOption))

  // Q_2.10.8
  println("Q_2.10.8:")

  println("Messages from HAL (2-6):")
  val messagesFromHal2to6 = messages.filter(_.sender === "HAL").map(_.content).drop(1).take(5)
  exec(messagesFromHal2to6.result) foreach {
    println
  }

  println("Messages from HAL (10-20):")
  val messagesFromHal10to20 = messages.filter(_.sender === "HAL").map(_.content).drop(9).take(11)
  exec(messagesFromHal10to20.result) foreach {
    println
  }

  // Q_2.10.9
  println("Q_2.10.9:")

  println("Message starting with Open:")
  val messageOpen = messages.filter(_.content startsWith "Open").map(_.content)
  println(exec(messageOpen.result))
  println(s"SQL: ${messageOpen.result.statements.head}")

  // Q_2.10.10
  println("Q_2.10.10:")

  println("Message containing do:")
  val messagesDo = messages.filter(_.content.toLowerCase like "%do%").map(_.content)
  exec(messagesDo.result) foreach { println }
  println(s"SQL: ${messagesDo.result.statements.head}")

  // Q_2.10.11
  println("Messages + !:")
  exec(messages.map(_.content + "!").result) foreach { println }
  println(s"SQL: ${messages.map(_.content + "!").result.statements.head}")

  println("Messages ++ ! (Take 1):")
  exec(messages.map(m => m.content ++ LiteralColumn("!")).result) foreach { println }

  println("Messages ++ ! (Take 2):")
  exec(messages.map(m => m.content ++ "!").result) foreach { println }
}