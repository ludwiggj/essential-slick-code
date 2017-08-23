package exercises

import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Unfolding extends App {

  // Database connection details:
  val db = Database.forConfig("chapter04")

  // Helper method for running a query in this example file:
  def exec[T](program: DBIO[T]): T =
    Await.result(db.run(program), 5000 milliseconds)

  final case class Room(name: String, connectsTo: String)

  final class FloorPlan(tag: Tag) extends Table[Room](tag, "floorplan") {
    def name = column[String]("name")

    def connectsTo = column[String]("next")

    def * = (name, connectsTo).mapTo[Room]
  }

  lazy val floorplan = TableQuery[FloorPlan]

  exec {
    (floorplan.schema.create) >>
      (floorplan += Room("Outside", "Podbay Door")) >>
      (floorplan += Room("Podbay Door", "Podbay")) >>
      (floorplan += Room("Podbay", "Galley")) >>
      (floorplan += Room("Galley", "Computer")) >>
      (floorplan += Room("Computer", "Engine Room"))
  }

  // Utility to print out what is in the database:
  def printCurrentDatabaseState() = {
    println("\nState of the database:\n")
    exec(floorplan.result.map(_.foreach(println)))
    println
  }

  try {
    printCurrentDatabaseState()

    // Write a method unfold that will take any room name as a starting point, and a query
    // to find the next room, and will follow all the connections until there are no more
    // connecting rooms.

    // The signature of unfold could be:
    //   z is the starting (“zero”) room
    //   f will lookup the connecting room (an action for the query to find the next room)

    def unfoldWithoutAcc(z: String, f: String => DBIO[Option[String]]): DBIO[Seq[String]] = ???

    // If unfold is given "Podbay" as a starting point it should return an action which, when run,
    // will produce: Seq("Podbay", "Galley", "Computer", "Engine Room").

    // You’ll want to accumulate results of the rooms you visit. One way to do that would be to
    // use a different signature:

    def unfold(z: String, f: String => DBIO[Option[String]], acc: Seq[String] = Seq.empty): DBIO[Seq[String]] = {
      f(z) flatMap {
        case Some(nextRoom) => unfold(nextRoom, f, acc :+ nextRoom)
        case None => DBIO.successful(acc)
      }
    }

    def nextRoom(currentRoom: String): DBIO[Option[String]] = {
      floorplan.filter(_.name === currentRoom).map(_.connectsTo).result.headOption
    }

    println(exec(nextRoom("Outside")))
    println(exec(nextRoom("Engine Room")))

    println(exec(unfold("Podbay", nextRoom, Seq("Podbay"))))

    def unfoldTextbook(z: String, f: String => DBIO[Option[String]], acc: Seq[String] = Seq.empty): DBIO[Seq[String]] = {
      f(z).flatMap {
        case Some(nextRoom) => unfoldTextbook(nextRoom, f, acc :+ z)
        case None => DBIO.successful(acc :+ z)
      }
    }

    println(exec(unfoldTextbook("Podbay", nextRoom)))

  } finally db.close
}