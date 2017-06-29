package chapter01.exercises

import chapter01.{Helper, Message, MessageTable}
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Q1 extends App {
  lazy val messages = TableQuery[MessageTable]

  // Create an in-memory H2 database;
  val db = Database.forConfig("chapter01")

  // Helper method for running a query in this example file:
  def exec[T](program: DBIO[T]): T = {
    val eventualT: Future[T] = db.run(program)
    val result = Await.result(eventualT, 2 seconds)
    println(s"Result: $result")
    result
  }

  private val createAction: DBIO[Unit] = messages.schema.create
  private val insertRowsAction: DBIO[Option[Int]] = messages ++= Helper.freshTestData
  private val selectAction: DBIO[Seq[Message]] = messages.result
  private val insertRowAction: DBIO[Int] = messages += Message("Dave", "What if I say 'Pretty please'?")
  private val selectDaveAction: DBIO[Seq[Message]] = messages.filter(_.sender === "Dave").result

  exec(createAction andThen insertRowsAction andThen selectAction andThen insertRowAction andThen selectDaveAction)
}
