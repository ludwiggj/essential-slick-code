package chapter05.aggregates

import chapter05.ChatSchema.Schema

object GroupingExample extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>

      populate

      // How many messages has each user sent?
      val msgPerUser =
        messages.groupBy(_.senderId).map { case (senderId, msgs) => senderId -> msgs.length }.run

      msgPerUser.foreach(println(_))

      // It’d be nicer to see the user’s name

      val msgsPerUser =
        messages.join(users).on(_.senderId === _.id).
          groupBy { case (msg, user) => user.name }.
          map { case (name, group) => name -> group.length }.
          run

      println(s"Messages per user:")
      msgsPerUser.foreach(println(_))

      // A more involved example, collecting some stats about our messages.
      // We want to find, for each user, how many messages they sent, and
      // the date of their first message. We want a result something like this:

      // Vector(
      //   (Frank, 2, Some(2001-02-16T20:55:00.000Z)),
      //   (HAL, 2, Some(2001-02-17T10:22:52.000Z)),
      //   (Dave, 4, Some(2001-02-16T20:55:04.000Z))
      // )

      val stats =
        messages.join(users).on(_.senderId === _.id).
          groupBy { case (msg, user) => user.name }.
          map { case (name, group) => (name, group.length, group.map { case (msg, user) => msg.ts }.min) }

      println(s"Stats:")
      stats.run.foreach(println(_))

      // We can simplify this, but before doing so, it may help to clarify that this query is equivalent
      // to the following SQL:

      // select user.name, count(1), min(message.ts)
      // from message inner join user on message.sender = user.id
      // group by user.name

      // Convince yourself the Slick and SQL queries are equivalent, by comparing:
      //      • the map expression in the Slick query to the SELECT clause in the SQL;
      //      • the join to the SQL INNER JOIN; and
      //      • the groupBy to the SQL GROUP expression.

      // These kinds of queries can be simplified by introducing intermediate functions with meaningful names.
      // There are a few ways to go at simplifying this, but the lowest hanging fruit is that min expression
      // inside the map.

      // The issue here is that the type of the group variable in the above query is:
      //
      // Query[(MessageTable, UserTable), (Message, User), Seq]
      //
      // That leads to us having to split it further to access the message’s timestamp field i.e.

      // map { case (name, group) => (name, group.length, group.map { case (msg, user) => msg.ts }.min) }

      // Let’s pull that part out as a method:

      import scala.language.higherKinds

      def timestampOf[S[_]](group: Query[(MessageTable, UserTable), (Message, User), S]) =
        group.map { case (msg, user) => msg.ts }

      // The query (group) is parameterized by the join, the unpacked values, and the container for the results.
      // By container we mean something like Vector[T] (from "run"-ing the query) or List[T] (if we list the
      // query). We don’t really care what our results go into, but we do care we’re working with messages and
      // users. The query becomes:

      val nicerStats =
        messages.join(users).on(_.senderId === _.id).
          groupBy { case (msg, user) => user.name }.
          map { case (name, group) => (name, group.length, timestampOf(group).min) }

      println(s"Nicer stats:")
      nicerStats.run.foreach(println(_))

      // Group by true

      // There’s a groupBy { _ => true} trick you can use where you want to select more than one aggregate
      // from a query.

      // e.g. select min(ts), max(ts) from message where content like '%read%'

      // It's easy to get min or max...

      messages.filter(_.content like "%read%").map(_.ts).min.run.foreach(println(_))

      // The key is to group all rows into the same group, which allows us to reuse the msgs collection

      messages.filter(_.content like "%read%").groupBy(_ => true).map {
        case (_, msgs) => (msgs.map(_.ts).min, msgs.map(_.ts).max)
      }.run.foreach(println(_))


      // Grouping by multiple columns

      // The result of groupBy can be a tuple, which gives access to grouping by multiple columns

      // We can look at the number of messages per user per room. Something like this:

      // Vector(
      //   (Air Lock, HAL, 2),
      //   (Air Lock, Dave, 2),
      //   (Pod, Dave, 2),
      //   (Pod, Frank, 2)
      // )

      // That is, we need to group by room and then by user, and finally count the number of rows in each group:

      val msgsPerRoomPerUser =
        rooms.
          // We join on messages, room and user to be able to display the room title and user name
          join(messages).on(_.id === _.roomId).
          join(users).on { case ((room, msg), user) => user.id === msg.senderId }.
          // The value passed into the groupBy will be determined by the join
          // The result of the groupBy is the columns for the grouping, which is a tuple of the room
          // title and the user's name.
          groupBy { case ((room, msg), user) => (room.title, user.name) }.
          // We select just the columns we want: room, user and the number of rows.
          map { case ((room, user), group) => (room, user, group.length) }.
          // sortBy to place the results in room order
          sortBy { case (room, user, group) => room }.
          run

      println(s"Messages per room per user:")
      msgsPerRoomPerUser.foreach(println(_))
  }
}