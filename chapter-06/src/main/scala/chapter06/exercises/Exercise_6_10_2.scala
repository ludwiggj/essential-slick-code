package chapter06.exercises

import chapter06.exercises.ChatSchema.Schema

import scala.concurrent.Await
import scala.concurrent.duration._

object Exercise_6_10_2 extends App {

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  val db = Database.forConfig("chapter06")

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  println(s"query result ${exec(populate)}")

  def findByName(name: String): Query[Rep[Message], Message, Seq] = {
    users.filter(_.name === name).join(messages).on(_.id === _.senderId).map { case (_, m) => m }
  }

  def findByName2(name: String): Query[Rep[Message], Message, Seq] = for {
    u <- users if u.name === name
    m <- messages if m.senderId === u.id
  } yield m

  println(s"\nMessages sent by HAL:\n" + s"${exec(findByName("HAL").result)}")
  println(s"\nMessages sent by HAL:\n" + s"${exec(findByName2("HAL").result)}")
}