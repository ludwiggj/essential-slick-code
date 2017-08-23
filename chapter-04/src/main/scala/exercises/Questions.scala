package exercises

import chapter04.Helper._
import chapter04.Message
import chapter04.Workout.messages
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

object Questions extends App {

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
    // Q 4.6.1 - and then what?
    println("Q 4.6.1 - and then what?")

    val drop: DBIO[Unit] = messages.schema.drop
    val create: DBIO[Unit] = messages.schema.create
    val populate: DBIO[Option[Int]] = messages ++= testData

    // Using andThen
    val recreateDb = drop.asTry andThen create andThen populate
    exec(recreateDb)
    printCurrentDatabaseState()

    // Using seq
    val recreateDb2 = DBIO.seq(drop.asTry, create, populate)
    exec(recreateDb2)
    printCurrentDatabaseState()

    // Recreate empty db
    val recreateEmptyDb = DBIO.seq(drop.asTry, create)
    exec(recreateEmptyDb)

    // Q 4.6.2 - first!
    println("Q 4.6.2 - first!")

    println("First the hard way...")

    def insert(m: Message): DBIO[Int] = {

      val insertAndCount = (messages += m) andThen messages.length.result

      insertAndCount.flatMap {
        case 1 => {
          messages.result.head.flatMap {
            msg => messages.map(_.content).update("First!" + msg.content)
          }
        }

        case _ => {
          DBIO.successful(0)
        }
      }
    }

    println(exec(insert(Message("Bob", "Hi"))))
    println(exec(insert(Message("Job", "Yo"))))

    printCurrentDatabaseState()

    println("Now the easier way...")

    exec(recreateEmptyDb)

    def insertImproved(m: Message): DBIO[Int] = {

      messages.length.result.flatMap {
        case 0 => {
          messages += m.copy(content = "First!" + m.content)
        }

        case _ => {
          messages += m
        }
      }
    }

    println(exec(insertImproved(Message("Bo", "Hi"))))
    println(exec(insertImproved(Message("Jo", "Yo"))))

    printCurrentDatabaseState()

    exec(recreateDb)

    // Q 4.6.3 - there can only be one...
    println("Q 4.6.3 - there can only be one...")

    def onlyOne[T](ms: DBIO[Seq[T]]): DBIO[T] = {
      ms.flatMap {
        seq =>
          seq.size match {
            case 1 => DBIO.successful(seq.head)
            case n => DBIO.failed(new RuntimeException(s"Expected 1 result, not ${n}"))
          }
      }
    }

    println(exec(onlyOne(messages.filter(_.content like "%sorry%").result)))

    // Comment this out otherwise the rest doesn't execute
    // println(exec(onlyOne(messages.filter(_.content like "%HAL%").result)))

    // Q 4.6.4 - let's be reasonable
    println("Q 4.6.4 - let's be reasonable")

    // Implement exactlyOne which wraps onlyOne encoding the possibility of failure using types rather than exceptions.

    def exactlyOne[T](ms: DBIO[Seq[T]]): DBIO[Try[T]] = {
      onlyOne(ms).asTry
    }

    println(exec(exactlyOne(messages.filter(_.content like "%sorry%").result)))
    println(exec(exactlyOne(messages.filter(_.content like "%HAL%").result)))

    // Q 4.6.5 - filter
    println("Q 4.6.5 - filter")

    def myFilter[T](action: DBIO[T])(p: T => Boolean)(alternative: => T): DBIO[T] = {
      action map {
        result => if (p(result)) result else alternative
      }
    }

    def textbookFilter[T](action: DBIO[T])(p: T => Boolean)(alternative: => T): DBIO[T] = {
      action map {
        case t if p(t) => t
        case _ => alternative
      }
    }

    println(exec(myFilter(messages.size.result)(_ < 100)(100)))
    println(exec(myFilter(messages.size.result)(_ < 4)(0)))

    println(exec(textbookFilter(messages.size.result)(_ < 100)(100)))
    println(exec(textbookFilter(messages.size.result)(_ < 4)(0)))

  } finally db.close
}