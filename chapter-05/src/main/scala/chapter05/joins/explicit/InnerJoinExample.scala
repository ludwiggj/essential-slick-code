package chapter05.joins.explicit

import chapter05.ChatSchema._

object InnerJoinExample extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>
      populate

      // Four varieties of join:

      // 1) Inner join

      // Inner join: we select records from multiple tables, where those records exist (in some
      // sense) in all tables.

      val daveId: PK[UserTable] = users.filter(_.name === "Dave").map(_.id).first
      val airLockId: PK[RoomTable] = rooms.filter(_.title === "Air Lock").map(_.id).first

      // All messages sent by Dave to airlock, with each query part separated out.
      // As queries in Slick compose, this works out nicely.
      val inner = messages.
        innerJoin(users).on(_.senderId === _.id).
        innerJoin(rooms).on { case ((msg, user), room) => msg.roomId === room.id }

      val daveInAirlockQ1 = for {
        ((msgs, usrs), rms) <- inner
        if usrs.id === daveId && rms.id === airLockId
      } yield (msgs.content, usrs.name, rms.title)

      daveInAirlockQ1.run.foreach(result => println(result))

      // You might prefer to inline inner within the query

      val daveInAirlockQ2 = for {
        ((msgs, usrs), rms) <- messages.
          innerJoin(users).on(_.senderId === _.id).
          innerJoin(rooms).on { case ((msg, _), room) => msg.roomId === room.id }
        if usrs.id === daveId && rms.id === airLockId
      } yield (msgs.content, usrs.name, rms.title)

      daveInAirlockQ2.run.foreach(result => println(result))

      // Concise version
      val innerConcise = messages.
        innerJoin(users).on(_.senderId === _.id).
        innerJoin(rooms).on(_._1.roomId === _.id)

      val daveInAirlockQ3 = for {
        ((msgs, usrs), rms) <- innerConcise
        if usrs.id === daveId && rms.id === airLockId
      } yield (msgs.content, usrs.name, rms.title)

      daveInAirlockQ3.run.foreach(result => println(result))
  }
}