package chapter06.exercises

import chapter06.exercises.ChatSchema.Schema

import scala.concurrent.Await
import scala.concurrent.duration._

object Exercise_6_10_3 extends App {

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  val db = Database.forConfig("chapter06")

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  println(s"query result ${exec(populate)}")

  val msgsPerUser =
    messages.join(users).on(_.senderId === _.id)
      .groupBy { case (msg, user) => user.name }
      .map { case (name, group) => name -> group.length }

  println(s"\nMessages per user:\n" + s"${exec(msgsPerUser.result)}")

  val havingManyMessages = msgsPerUser.filter { case (name, count) => count > 2 }

  println(s"\nMany messages per user:\n" + s"${exec(havingManyMessages.result)}")
}