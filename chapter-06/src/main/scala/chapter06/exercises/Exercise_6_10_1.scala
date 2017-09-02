package chapter06.exercises

import chapter06.exercises.ChatSchema.Schema
import scala.concurrent.Await
import scala.concurrent.duration._

object Exercise_6_10_1 extends App {

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  val db = Database.forConfig("chapter06")

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  println(s"query result ${exec(populate)}")

  val messagesAndSendersWithFK = for {
    msg <- messages
    usr <- users
    if (msg.senderId === usr.id)
  } yield (msg, usr)

  println(s"\nMessages and sender details FK:\n${exec(messagesAndSendersWithFK.result)}")

  val messagesAndSenders = for {
    msg <- messages
    usr <- msg.sender
  } yield (msg, usr)

  println(s"\nMessages and sender details:\n${exec(messagesAndSenders.result)}")

  val messageContentAndSenderNames = (for {
    msg <- messages
    usr <- msg.sender
  } yield (msg.content, usr.name))

  println(s"\nMessage content and sender name:\n${exec(messageContentAndSenderNames.result)}")

  val messageContentAndSenderNamesInNameOrder = messageContentAndSenderNames.sortBy { case (_, name) => name }

  println(s"\nMessage content and sender name, ascending name order:\n" +
    s"${exec(messageContentAndSenderNamesInNameOrder.result)}")

  val messageContentAndSenderNamesInNameOrder2 =
    messages.join(users).on(_.senderId === _.id).map { case (m, u) => (m.content, u.name) }
      .sortBy { case (_, name) => name }

  println(s"\nMessage content and sender name, ascending name order:\n" +
    s"${exec(messageContentAndSenderNamesInNameOrder2.result)}")
}