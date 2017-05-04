package chapter05.joins.zip

import chapter05.ChatSchema._

object ZipJoinExample extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter05", driver = "org.h2.Driver")

  db.withSession {
    implicit session =>
      populate

      // Zip Join

      // Pair up adjacent messages into a "conversation":

      // Select messages, ordered by the date the messages were sent
      val msgs = messages.sortBy(_.ts asc)

      // Pair up adjacent messages:
      val conversations = msgs zip msgs.drop(1)

      // Select out just the contents of the first and second messages:
      val zipResults: List[(String, String)] =
        conversations.map { case (fst, snd) => fst.content -> snd.content }.list

      // This will turn into an inner join
      zipResults.foreach(println(_))

      // A second variation, zipWith, allows you to give a mapping function along
      // with the join. We could have written the above as:

      def combiner(fst: MessageTable, snd: MessageTable) = fst.content -> snd.content

      val results = msgs.zipWith(msgs.drop(1), combiner).list

      results.foreach(println(_))

      // The final variant is zipWithIndex, which is as per the Scala collections method
      // of the same name. Let’s number each message:
      val zippedWithIndexResult = messages.zipWithIndex.map {
        case (msg, index) => index -> msg.content
      }.list

      zippedWithIndexResult.foreach(println(_))
  }
}