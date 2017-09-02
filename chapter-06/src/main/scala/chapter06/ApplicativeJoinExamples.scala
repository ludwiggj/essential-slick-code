package chapter06

import slick.lifted.BaseJoinQuery

import scala.concurrent.Await
import scala.concurrent.duration._

object ApplicativeJoinExamples extends App {

  def basicSchemaExamples() {
    import schemas.ChatSchemaBasic.Schema

    val schema = new Schema(slick.jdbc.H2Profile)

    import schema._
    import profile.api._

    val db = Database.forConfig("chapter06")

    def exec[T](action: DBIO[T]): T =
      Await.result(db.run(action), 2 seconds)

    println(s"query result ${exec(populate)}")

    val q1: Query[(MessageTable, UserTable), (Message, User), Seq] =
      messages join users on (_.senderId === _.id)

    println(s"\nMessages:\n${exec(q1.result)}")

    val q2: Query[(MessageTable, UserTable), (Message, User), Seq] =
      messages join users on ((m: MessageTable, u: UserTable) => m.senderId === u.id)

    println(s"\nMessages:\n${exec(q2.result)}")

    val q3: Query[(MessageTable, UserTable), (Message, User), Seq] =
      messages join users on { case (m, u) => m.senderId === u.id }

    println(s"\nMessages:\n${exec(q3.result)}")

    val q4 = messages.join(users).on(_.senderId === _.id).map { case (msg, user) => (msg.content, user.name) }
    exec(q4.result).foreach(println)

    println(s"dropping tables ${exec(dropSchema)}")
  }

  def fullSchemaExamples() = {
    import schemas.ChatSchemaFull.Schema

    val schema = new Schema(slick.jdbc.H2Profile)

    import schema._
    import profile.api._

    val db = Database.forConfig("chapter06")

    def exec[T](action: DBIO[T]): T =
      Await.result(db.run(action), 2 seconds)

    println(s"query result ${exec(populate)}")

    def innerJoins() {
      val usersAndRooms =
        messages.
          join(users).on(_.senderId === _.id).
          join(rooms).on { case ((msg, user), room) => msg.roomId === room.id }

      exec(usersAndRooms.result).foreach(println)

      val usersAndRooms2 =
        messages.
          join(users).on(_.senderId === _.id).
          join(rooms).on(_._1.roomId === _.id)

      exec(usersAndRooms2.result).foreach(println)

      val usersAndRooms3 =
        messages.
          join(users).on(_.senderId === _.id).
          join(rooms).on { case ((msg, user), room) => msg.roomId === room.id }.
          map { case ((msg, user), room) => (msg.content, user.name, room.title) }

      exec(usersAndRooms3.result).foreach(println)

      // Filter
      val airLockMsgs = usersAndRooms.filter { case (_, room) => room.title === "Air Lock" }

      exec(airLockMsgs.result).foreach(println)

      val daveId = users.filter(_.name === "Dave").map(_.id)
      val halId = users.filter(_.name === "HAL").map(_.id)
      val airLockId = rooms.filter(_.title === "Air Lock").map(_.id)

      val daveInAirLock = for {
        dId <- daveId
        rId <- airLockId
        ((msgs, usrs), rms) <- usersAndRooms2
        if usrs.id === dId && rms.id === rId
      } yield (msgs.content, usrs.name, rms.title)

      exec(daveInAirLock.result).foreach(println)
    }

    def leftJoins() = {
      val left: Query[(MessageTable, Rep[Option[RoomTable]]), (MessageTable#TableElementType, Option[Room]), Seq] =
        messages.joinLeft(rooms).on(_.roomId === _.id)

      exec(left.result).foreach(println)

      // If we want to just pick out the message content and the room title, we can map over the query:
      val leftMapped: Query[(Rep[String], Rep[Option[String]]), (String, Option[String]), Seq] =
        messages.joinLeft(rooms).on(_.roomId === _.id).map {
          case (msg, room) => (msg.content, room.map(_.title))
        }

      exec(leftMapped.result).foreach(println)

      val messageRecipients = messages.joinLeft(users).on(_.toId === _.id).
        map { case (m, u: Rep[Option[UserTable]]) => (m.content, u.map(_.name)) }

      exec(messageRecipients.result).foreach(println)
    }

    def rightJoin() = {
      val right: Query[(Rep[String], Rep[Option[String]]), (String, Option[String]), Seq] = for {
        (msg, room) <- messages joinRight (rooms) on (_.roomId === _.id)
      } yield (room.title, msg.map(_.content))

      exec(right.result).foreach(println)

      val messagesReceivedBy = for {
        (msg, user) <- messages.joinRight(users).on(_.toId === _.id)
      } yield (user.name, msg.map(_.content))

      exec(messagesReceivedBy.result).foreach(println)
    }

    def fullOuterJoin() = {
      val outer: Query[(Rep[Option[String]], Rep[Option[String]]), (Option[String], Option[String]), Seq] = for {
        (room, msg) <- rooms joinFull messages on (_.id === _.roomId)
      } yield (room.map(_.title), msg.map(_.content))

      exec(outer.result).foreach(println)
    }

    def crossJoin() {
      val cross: BaseJoinQuery[MessageTable, Rep[Option[UserTable]], MessageTable#TableElementType, Option[User], Seq, MessageTable, UserTable] =
        messages joinLeft users

      exec(cross.result).foreach(println)
    }

    def zipJoins() = {
      val msgs = messages.sortBy(_.id.asc).map(_.content)
      val conversation1 = msgs zip msgs.drop(1)
      exec(conversation1.result).foreach(println)

      val fullConversation = messages zip messages.drop(1)
      val conversation2 = fullConversation.map { case (fst, snd) => fst.content -> snd.content }
      exec(conversation2.result).foreach(println)

      def combiner(c1: Rep[String], c2: Rep[String]) = (c1.toUpperCase, c2.toLowerCase)

      val conversation3 = msgs.zipWith(msgs.drop(1), combiner)
      exec(conversation3.result).foreach(println)

      def anotherCombiner(fst: MessageTable, snd: MessageTable) = fst.content -> snd.content

      val conversation4 = messages.zipWith(messages.drop(1), anotherCombiner)
      exec(conversation4.result).foreach(println)

      val conversation5 = messages.map(_.content).zipWithIndex
      val action: DBIO[Seq[(String, Long)]] = conversation5.result
      exec(action).foreach(println)

      exec(messages.zipWithIndex.map {
        case (msg, index) => index -> msg.content
      }.result).foreach(println)
    }

    innerJoins()
    leftJoins()
    rightJoin()
    fullOuterJoin()
    crossJoin()
    zipJoins()
  }

  basicSchemaExamples()
  fullSchemaExamples()
}