package chapter05.joins.explicit

import chapter05.ChatSchema._

object RightJoinExample extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>
      populate

      // Four varieties of join:

      // 3) rightJoin — a right outer join

      // In the left join we selected all the records from one side of the join, with possibly
      // NULL values from the other tables. The right join (or right outer join) reverses this.
      // We can switch the example for left join and ask for all users, what private messages
      // have they received.

      val right = for {
        (msg, user) <- messages.rightJoin(users).on(_.toId === _.id)
      } yield (user.name, msg.content.?)

      right.run.foreach(result => println(result))
  }
}