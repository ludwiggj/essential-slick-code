package chapter05.joins.explicit

import chapter05.ChatSchema._

object FullOuterJoinExample extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>
      populate

      // Four varieties of join:

      // 4) outerJoin — a full outer join

      // H2 doesn't support FULL OUTER JOINS at the time of writing.

      // A simple example would be as below.

      // The query finds the title of all room and messages in those rooms.
      // Either side could be NULL because messages don’thave to be in rooms,
      // and rooms don’t have to have any messages.

      // Running it results in:

      // scala.slick.SlickException: Expected a collection type, found UnassignedType

      val outer = for {
        (room, msg) <- rooms outerJoin messages on (_.id === _.roomId)
      } yield room.title.? -> msg.content.?

      outer.run.foreach(println)
  }
}