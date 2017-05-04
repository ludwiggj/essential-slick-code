package chapter05.joins.explicit

import chapter05.ChatSchema._

object ExplicitJoinIntro extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>
      populate

      // Explicit Joins, four varieties of join:

      // 1) innerJoin or join — an inner join
      // 2) leftJoin — a left outer join
      // 3) rightJoin — a right outer join
      // 4) outerJoin — a full outer join

      // Taste of the syntax:
      val syntax: Query[(MessageTable, UserTable), (Message, User), Seq] =
        messages innerJoin users on (_.senderId === _.id)

      val syntaxQuery = syntax.map { case (msg, user) => (msg.content, user.name) }
      syntaxQuery.run.foreach(result => println(result))

      // If we wanted to, we could be more explicit about the values used in the on part:
      val q = messages innerJoin users on ((m: MessageTable, u: UserTable) => m.senderId === u.id)
  }
}