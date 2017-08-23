package chapter04

import chapter04.Helper.recreateAndPopulateDb
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Workout extends App {

  // Table:
  lazy val messages = TableQuery[MessageTable]

  // Database connection details:
  val db = Database.forConfig("chapter04")

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
    exec(recreateAndPopulateDb)

    printCurrentDatabaseState()

    println("andThen example; last result is returned...\n")

    // Use andThen to combine actions, all but last result discarded
    val reset: DBIO[Int] = messages.delete andThen messages.size.result

    println(s"Deleted all rows, number left: [${exec(reset)}]")

    printCurrentDatabaseState()

    exec(recreateAndPopulateDb)

    printCurrentDatabaseState()

    // DBIO.seq combines actions, all results discarded

    println("DBIO.seq example; all results discarded...\n")

    val reset2: DBIO[Unit] = DBIO.seq(messages.delete, messages.size.result)

    exec(reset2)

    println("Deleted all rows")

    printCurrentDatabaseState()

    exec(recreateAndPopulateDb)

    printCurrentDatabaseState()

    // Mapping over an action is a way to set up a transformation of a value from the
    // database. The transformation will run on the result of the action when it is returned
    // by the database.

    // Map
    println("Map, reversing message from the database...\n")

    val textAction: DBIO[Option[String]] =
      messages.map(_.content).result.headOption

    val reversed: DBIO[Option[String]] =
      textAction.map(maybeText => maybeText.map(_.reverse))

    println(s"Reversed message: ${exec(reversed)}")

    val textLength: DBIO[Option[Int]] = textAction.map(maybeText => maybeText.map(_.length))

    println(s"Reversed message length: ${exec(textLength)}")

    // FlatMap:
    println("\nFlatMap, delete all messages and insert message to state how many were removed...")

    val delete: DBIO[Int] = messages.delete

    def insert(count: Int) = messages += Message("NOBODY", s"I removed ${count} messages")

    val resetMessagesAction: DBIO[Int] = delete.flatMap { count => insert(count) }

    exec(resetMessagesAction)

    printCurrentDatabaseState()

    println("As before, but only inserting message if at least one message was removed in the first step...\n")

    val resetMessagesAction2: DBIO[Int] =
      delete.flatMap {
        case 0 => {
          println("No messages deleted")
          DBIO.successful(0)
        }
        case n => {
          println(s"${n} messages deleted")
          insert(n)
        }
      }

    println("case (1) FlatMap with an empty database...")

    exec(messages.delete)

    printCurrentDatabaseState()

    exec(resetMessagesAction2)

    printCurrentDatabaseState()

    println("case (2) FlatMap with a non-empty database...")

    exec(recreateAndPopulateDb)

    printCurrentDatabaseState()

    exec(resetMessagesAction2)

    printCurrentDatabaseState()

    println("FlatMap to insert row if not present... (slower, but perhaps easier to understand)")

    def insertIfNotExists(m: Message): DBIO[Int] = {
      val alreadyExists =
        messages.filter(_.content === m.content).result.headOption
      alreadyExists.flatMap {
        case Some(m) => {
          println("Message already present!")
          DBIO.successful(0)
        }
        case None => {
          println("Adding message")
          messages += m
        }
      }
    }

    printCurrentDatabaseState()

    val pirateMessage = Message("Captain Pugwash", "Aha me hearties!")

    println(exec(insertIfNotExists(pirateMessage)))
    printCurrentDatabaseState()

    println(exec(insertIfNotExists(pirateMessage)))
    printCurrentDatabaseState()

    // DBIO.sequence

    println("DBIO.sequence")

    def reverse(msg: Message): DBIO[Int] = messages.filter(_.id === msg.id).map(_.content).update(msg.content.reverse)

    val updates: DBIO[Seq[DBIO[Int]]] = messages.result.map(msgs => msgs.map(reverse))

    val updates2: DBIO[Seq[Int]] = messages.result.flatMap(msgs => DBIO.sequence(msgs.map(reverse)))

    exec(recreateAndPopulateDb)

    printCurrentDatabaseState()

    exec(updates2)

    printCurrentDatabaseState()

    // Fold:
    println("Fold...")

    val report1: DBIO[Int] = DBIO.successful(41)
    val report2: DBIO[Int] = DBIO.successful(1)
    val reports: List[DBIO[Int]] = report1 :: report2 :: Nil

    val summary: DBIO[Int] = DBIO.fold(reports, 0) {
      (total, report) => total + report
    }

    println("\nSummary of all reports via fold:")
    println(exec(summary))

    // Zip - combine actions and keep all results
    println("\nZip....")

    exec(recreateAndPopulateDb)

    printCurrentDatabaseState()

    val countAndHal: DBIO[(Int, Seq[Message])] = messages.size.result zip messages.filter(_.sender === "HAL").result

    println("Zipped actions:\n")

    println(exec(countAndHal))

    println("\nCleanup...\n")

    // An action to record problems we encounter:
    def log(err: Throwable): DBIO[Int] = messages += Message("SYSTEM", err.getMessage)

    println("Cleanup a failing action...\n")

    val anAction: DBIO[Nothing] = DBIO.failed(new RuntimeException("pod bay door unexpectedly locked"))

    // anAction represents an action which might fail
    val action1 = anAction.cleanUp {
      case Some(err) => {
        println("Handling failing action")
        log(err)
      }
      case None => {
        println("Successful action, nowt to do here...")
        DBIO.successful(0)
      }
    }

    try {
      exec(action1)
    } catch {
      case e: RuntimeException => printCurrentDatabaseState()
    }

    println("Cleanup a successful action...\n")

    val anotherAction = DBIO.successful(0)

    // anotherAction represents an action which might fail
    val action2 = anotherAction.cleanUp {
      case Some(err) => {
        println("Handling failing action")
        log(err)
      }
      case None => {
        println("Successful action, nowt to do here...")
        DBIO.successful(0)
      }
    }

    try {
      exec(action2)
    } catch {
      case e: RuntimeException => printCurrentDatabaseState()
    }

    println("\nandFinally...\n")

    println("andFinally after a failing action...\n")

    def finalAction = {
      println("Final action running...")
      DBIO.successful(0)
    }

    try {
      exec(anAction.andFinally(finalAction))
    } catch {
      case e: RuntimeException => println(s"Exception caught, ${e.getMessage}")
    }

    println("\nandFinally after a successful action...\n")

    try {
      exec(anotherAction.andFinally(finalAction))
    } catch {
      case e: RuntimeException => println(s"Exception caught, ${e.getMessage}")
    }

    println("\nasTry...\n")

    // Calling asTry on an action changes the action’s type from a DBIO[T] to a DBIO[Try[T]]
    // This means you can work in terms of Scala’s Success and Failure instead of exceptions

    println(exec(DBIO.failed(new RuntimeException("Boom!")).asTry))

    println(exec(messages.size.result.asTry))

    // Transactions

    println("\nTransactions...")

    printCurrentDatabaseState()

    def updateContent(old: String) = messages.filter(_.content === old).map(_.content)

    println(exec {
      (
        updateContent("Affirmative, Dave. I read you.").update("Wanna come in?")
          andThen
          updateContent("Open the pod bay doors, HAL.").update("Pretty please!")
          andThen
          updateContent("I'm sorry, Dave. I'm afraid I can't do that.").update("Opening now.")
        ).transactionally
    })

    printCurrentDatabaseState()

    println("\nTransaction rollback...")

    val willRollback = (
      (messages += Message("HAL", "Daisy, Daisy...")) >>
        (messages += Message("Dave", "Please, anything but your singing")) >>
        DBIO.failed(new Exception("agggh my ears")) >>
        (messages += Message("HAL", "Give me your answer do"))
      ).transactionally

    println("\nResult from rolling back:")
    println(exec(willRollback.asTry))

    printCurrentDatabaseState()

  } finally db.close
}