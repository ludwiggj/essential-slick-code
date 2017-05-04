package chapter05.joins.explicit

import chapter05.ChatSchema._

object LeftJoinExample extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>
      populate

      // Four varieties of join:

      // 2) leftJoin — a left outer join

      // Messages can optionally be sent privately to another user.
      // We want a list of all the messages and who they were sent to (if any).

      // Note the u.name.? expression below is required to turn the potentially
      // null result from the query into an Option value. You need to deal with
      // this on a column-by-column basis. This is a bit of a pain and is
      // improved in Slick 3
      val left = messages.
        leftJoin(users).on(_.toId === _.id).
        map { case (m, u) => (m.content, u.name.?) }

      left.run.foreach(result => println(result))

      // There’s a way to tell if you’ve got it wrong. Take a look at this query,
      // which is trying to list users and the rooms they are occupying:
      val outer = for {
        (usrs, occ) <- users leftJoin occupants on (_.id === _.userId)
      } yield usrs.name -> occ.roomId

      // Running this generates a slick exception...
      // scala.slick.SlickException: Read NULL value (null) for ResultSet column Path s2._2
      // outer.run.foreach(result => println(result))

      // This is due to users not having to be in a room. In the test data, one user (Elena)
      // has not been assigned to any room

      // The fix is to manually add .? to the selected column:

      val outerFixed = for {
        (usrs, occ) <- users leftJoin occupants on (_.id === _.userId)
      } yield usrs.name -> occ.roomId.?

      outerFixed.run.foreach(result => println(result))

      // Without an `on`, you have a cross join:
      (messages leftJoin users).run.foreach(println)
  }
}