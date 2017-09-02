package chapter06.exercises

import chapter06.exercises.ChatSchema.Schema

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Exercise_6_10_4 extends App {

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  val db = Database.forConfig("chapter06")

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  println(s"query result ${exec(populate)}")

  val msgsPerUser =
    users.join(messages).on(_.id === _.senderId)

  println(s"\nMessages per user:\n" + s"${exec(msgsPerUser.result)}")

  val almost = Seq(
    ("HAL" -> "Hello"),
    ("Dave" -> "How are you?"),
    ("HAL" -> "I have terrible pain in all the diodes")
  ).groupBy { case (name, message) => name }

  println("\nAlmost:")
  almost.foreach(println)

  println("\nCorrect:")
  val correct = almost.mapValues { values =>
    values.map { case (name, msg) => msg }
  }
  correct.foreach(println)

  def userMessages: DBIO[Map[User, Seq[Message]]] =
    users.join(messages).on(_.id === _.senderId).result.map { rows =>
      rows.groupBy { case (user, message) => user }
        .mapValues(values => values.map { case (name, msg) => msg })
    }

  println("\nUser messages:")
  exec(userMessages).foreach(println)
}