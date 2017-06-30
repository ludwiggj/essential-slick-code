package chapter03

import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import Helper.{messages, recreateDb, repopulateDb}

object Example extends App {

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

    // -- INSERTS --

    // Insert one
    println("Insert a row...\n")
    val action = messages += Message("HAL", "No. Seriously, Dave, I can't let you in.")
    println(exec(action))
    println(s"SQL: ${action.statements.head}")

    printCurrentDatabaseState()

    // Forced insert
    println("Force insert a row with a specific id...\n")

    val forceInsertAction = messages forceInsert
      Message("HAL", "I'm a computer, what would I do with a Christmas card anyway?", 1000L)
    println(s"SQL: ${forceInsertAction.statements.head}")
    println(exec(forceInsertAction))
    println(exec(messages.filter(_.id === 1000L).result))

    printCurrentDatabaseState()

    // Insert one, returning the ID:
    println("Insert a row, returning the resulting id...\n")

    val id = exec((messages returning messages.map(_.id)) += Message("HAL", "I'm back"))
    println(s"The ID inserted was: $id")
    println(exec(messages.filter(_.id === 1001L).result.headOption))

    printCurrentDatabaseState()

    // Repeatable
    println("Insert a row, returning the resulting id, via repeatable statement...\n")
    lazy val messagesReturningId = messages returning messages.map(_.id)
    println(exec(messagesReturningId += Message("HAL", "Humans, eh.")))

    printCurrentDatabaseState()

    // Emulate returning whole message with id
    println("Insert, emulate returning whole message with id...\n")
    val messagesReturningRow = messages returning messages.map(_.id) into { (message, id) =>
      message.copy(id = id)
    }
    println(exec(messagesReturningRow += Message("Dave", "You're such a jerk.")))

    printCurrentDatabaseState()

    // Attempt to insert specific columns
    println(s"SQL: ${messages.map(_.sender).insertStatement}\n")

    // Exception in thread "main" org.h2.jdbc.JdbcSQLException:
    // NULL not allowed for column "content"; SQL statement:

    // println(exec(messages.map(_.sender) += "HAL"))

    // NOTE - this will work if other columns have default or automatic values

    // Slick also provides a batch version of messages returning..., including the into method.
    // We can use the messagesReturningRow query we defined last section and write:

    println("Batch insert...\n")

    def moreData = Seq(
      Message("HAL", "Yo D, wassup?"),
      Message("Dave", "I beg your pardon?")
    )

    exec(messagesReturningRow ++= moreData) foreach {
      println
    }

    printCurrentDatabaseState()

    // Insert row if it's not present
    val data = Query(("Stanley", "Cut!"))
    println(s"SQL (data): ${data.result.statements.mkString}")

    val exists = messages.filter(m => m.sender === "Stanley" && m.content === "Cut!").exists
    println(s"SQL (exists): ${exists.result.statements.mkString}")

    val selectExpression = data.filterNot(_ => exists)
    println(s"SQL (select): ${selectExpression.result.statements.mkString}")

    val twoColumns = messages.map(m => (m.sender, m.content))
    println(s"SQL (twoColumns): ${twoColumns.result.statements}")

    val insertIfNotThere = twoColumns.forceInsertQuery(selectExpression)
    println(s"SQL (shebang): ${insertIfNotThere.statements}")

    println(exec(insertIfNotThere))

    printCurrentDatabaseState()

    println(exec(insertIfNotThere))

    printCurrentDatabaseState()

    // -- DELETES --

    // Delete messages from HAL:
    println("Deleting messages from HAL:")
    val deleteMessagesFromHAL = messages.filter(_.sender === "HAL").delete
    val rowsDeleted = exec(deleteMessagesFromHAL)
    println(s"Rows deleted: $rowsDeleted")
    println(s"SQL: ${deleteMessagesFromHAL.statements.head}")

    printCurrentDatabaseState()

    // Can only call delete on a TableQuery - the following line doesn't compile
    // exec(messages.map(_.content).delete)

    // Repopulate the database:
    println("Repopulating HAL's messages:")
    exec(repopulateDb)

    printCurrentDatabaseState()

    // -- UPDATES --

    // Update HAL's name:
    println("Update HAL's name:")
    val selectQuery = messages.filter(_.sender === "HAL").map(_.sender)
    println(s"SQL: ${selectQuery.result.statements.head}")
    val updateQuery = selectQuery.update("HAL 9000")
    println(s"SQL: ${selectQuery.updateStatement}")
    println(s"SQL: ${updateQuery.statements.head}")

    println(s"Updated ${exec(updateQuery)} rows")

    printCurrentDatabaseState()

    // Update multiple columns
    println("Update multiple columns...")

    val multipleColsQuery = messages.filter(_.id === 1006L).map(message => (message.sender, message.content))
    println(s"SQL: ${multipleColsQuery.updateStatement}")

    val multipleColsAction: DBIO[Int] = multipleColsQuery.update(("HAL 9000", "Sure, Dave. Come right in."))

    exec(multipleColsAction)

    printCurrentDatabaseState()

    // Update with mapTo
    println("Update with mapTo...")

    case class NameText(name: String, text: String)
    val newValue = NameText("Dave", "Now I totally don't trust you.")
    val multipleColsQueryMapTo =
      messages.filter(_.id === 1006L).map(
        message => (message.sender, message.content).mapTo[NameText]
      ).update(newValue)

    println(s"SQL: ${multipleColsQueryMapTo.statements.head}")

    exec(multipleColsQueryMapTo)

    printCurrentDatabaseState()

    // TODO - See next chapter!

    // Client-side update:
    //    def exclaim(msg: Message): Message = msg.copy(content = msg.content + "!")
    //
    //    val all: DBIO[Seq[Message]] = messages.result
    //
    //    def modify(msg: Message): DBIO[Int] = messages.filter(_.id === msg.id).update(exclaim(msg))
    //
    //    val action: DBIO[Seq[Int]] = all.flatMap(msgs => DBIO.sequence(msgs.map(modify)))
    //    val rowCounts: Seq[Int] = exec(action)

  } finally db.close
}