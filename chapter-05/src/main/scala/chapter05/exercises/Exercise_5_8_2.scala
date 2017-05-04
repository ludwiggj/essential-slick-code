package chapter05.exercises

import chapter05.ChatSchema.Schema

object Exercise_5_8_2 extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>
      populate

      // In this chapter we saw this query:

      val outer = for {
        (usrs, occ) <- users leftJoin occupants on (_.id === _.userId)
      } yield usrs.name -> occ.roomId


      // It causes us a problem because not every user occupies a room. Can you find
      // another way to express this that doesn't cause this problem ?
      val outerCorrected = for {
        (usrs, occ) <- users leftJoin occupants on (_.id === _.userId)
      } yield usrs.name -> occ.roomId.?

      outerCorrected.run.foreach(println(_))

      // Output:

      // (Dave,Some(PK(1)))
      // (Dave,Some(PK(2)))
      // (HAL,Some(PK(1)))
      // (HAL,Some(PK(2)))
      // (Elena,None)
      // (Frank,Some(PK(2)))

      // Alternatively, a right join between users and occupants can help here. For a
      // row to exist in the occupant table it must have a room:
      val usersRooms = for {
        (usrs, occ) <- users rightJoin occupants on (_.id === _.userId)
      } yield usrs.name -> occ.roomId

      usersRooms.run.foreach(println(_))

      // Output:

      // (Dave,PK(1))
      // (HAL,PK(1))
      // (Dave,PK(2))
      // (Frank,PK(2))
      // (HAL,PK(2))
  }
}