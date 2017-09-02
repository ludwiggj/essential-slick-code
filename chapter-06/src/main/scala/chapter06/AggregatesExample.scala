package chapter06

import chapter06.schemas.ChatSchemaAggregates.PK
import org.joda.time._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.higherKinds

object AggregatesExample extends App {

  import schemas.ChatSchemaAggregates.Schema

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  val db = Database.forConfig("chapter06")

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  exec(populate)

  // Count:
  val numRows: Int = exec(messages.length.result)
  println(s"Total messages: ${numRows}")

  // Number of message senders:
  val senders: Int = exec(messages.map(_.senderId).distinct.length.result)
  println(s"Unique message senders: ${senders}")

  // First message date:
  val first: Option[DateTime] = exec(messages.map(_.ts).min.result)
  println(s"First sent: ${first}")

  // Last message date:
  val last = messages.map(_.ts).max.result
  println(s"Last sent: ${exec(last)}")

  // Group by:
  val msgsPerSenderId: DBIO[Seq[(PK[UserTable], Int)]] =
    messages.groupBy(_.senderId)
      .map { case (senderId, msgs) => senderId -> msgs.length }
      .result

  println(s"Messages per senderId: ${exec(msgsPerSenderId)}")

  val msgsPerUser =
    messages.join(users).on(_.senderId === _.id)
      .groupBy { case (msg, user) => user.name }
      .map { case (name, group) => name -> group.length }
      .result

  println(s"Messages per user: ${exec(msgsPerUser)}")

  // More involved grouping:
  val stats =
    messages.join(users).on(_.senderId === _.id)
      .groupBy { case (msg, user) => user.name }
      .map { case (name, group) =>
        (name, group.length, group.map { case (msg, user) => msg.ts }.min)
      }

  println(s"Stats: ${exec(stats.result)}")

  // Extracting functions:
  import scala.language.higherKinds

  def timestampOf[S[_]](group: Query[(MessageTable, UserTable), (Message, User), S]) =
    group.map { case (msg, user) => msg.ts }

  val nicerStats =
    messages.join(users).on(_.senderId === _.id)
      .groupBy { case (msg, user) => user.name }
      .map { case (name, group) =>
        (name, group.length, timestampOf(group).min)
      }

  println(s"Nicer Stats: ${exec(nicerStats.result)}")

  val minMax = messages.filter(_.content like "%read%")
    .groupBy(_ => true).map {
    case (_, msgs) => (msgs.map(_.id).min, msgs.map(_.id).max)
  }

  println(s"Min and Max: ${exec(minMax.result)}")

  // Grouping by multiple columns:
  val msgsPerRoomPerUser =
    rooms
      .join(messages).on(_.id === _.roomId)
      .join(users).on { case ((room, msg), user) => user.id === msg.senderId }
      .groupBy { case ((room, msg), user) => (room.title, user.name) }
      .map { case ((room, user), group) => (room, user, group.length) }
      .sortBy { case (room, user, group) => room }
      .result
  println(s"Messages per room per user: ${exec(msgsPerRoomPerUser)}")
}