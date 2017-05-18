package chapter06

import chapter06.ChatSchema._
import org.joda.time._

import scala.slick.jdbc.StaticQuery.interpolation
import scala.slick.jdbc._

object SelectExample extends App {

  val schema = new Schema(scala.slick.driver.H2Driver)

  import schema._
  import profile.simple._

  def db = Database.forURL("jdbc:h2:mem:chapter06", driver = "org.h2.Driver")

  db withSession { implicit session =>

    populate

    // Simple select examples

    val roomIdsQuery = sql"""select "id" from "room" """.as[Long]
    val roomIds = roomIdsQuery.list
    println(roomIds)

    val roomInfoQuery = sql""" select "id", "title" from "room" """.as[(Long, String)]
    val roomInfo = roomInfoQuery.list
    println(roomInfo)

    // Referring to a scala value in the query
    val t = "Pod"
    println(
      sql""" select "id", "title" from "room" where "title" = $t""".as[(Long, String)].firstOption
    )

    val oops = 42
    // Type mismatch in following statement causes runtime error
    // JdbcSQLException: Data conversion error converting "Air Lock";
    //    println(
    //      sql""" select "id" from "room" where "title" = $oops """.as[Long].firstOption
    //    )

    // Another place you can unstuck is with the #$ style of substitution. This is used when you
    // don't want SQL escaping to apply. For example, perhaps the name of the table you want to
    // use may change:

    val table = "message"
    val query = sql""" select "id" from "#$table" """.as[Long]

    // In this situation we do not want the value of table to be treated as a String. That would
    // give you the invalid query: select "id" from "'message'". However, using this construct
    // means you can produce dangerous SQL. The golden rule is to never use #$ with input
    // supplied by a user.

    // query is a StaticQuery, it does not compose

    query.list.foreach(println(_))

    // Building up queries

    // Available operations are:
    // + to append a string to the query, giving a new query
    // +? to add a value, and correctly escape the value for use in SQL.

    // As an example, we can find all IDs for messages
    val messageIds = sql"""SELECT "id" from "message"""".as[Long]

    println(messageIds.list)

    //Now create a new query based on this to filter by message content:
    val pattern = "%Dave%"
    val sensitive = query + """ WHERE "content" NOT LIKE """ +? pattern

    println(sensitive.list)

    // Trying to work with a custom type, DateTime, without an implicit conversion, results in exception:
    // Error:(78, 47) could not find implicit value for parameter rconv:
    // scala.slick.jdbc.GetResult[org.joda.time.DateTime]

    //    println(
    //      sql""" select "ts" from "message" """.as[DateTime].list
    //    )

    // Get Result instance for DateTime

    implicit val GetDateTime = GetResult[DateTime](r => new DateTime(r.nextTimestamp(), DateTimeZone.UTC))

    println(
      sql""" select "ts" from "message" """.as[DateTime].list
    )

    // Returning a case class from a Plain SQL query means providing a GetResult for the case class

    // An example, using the Message case class. Definition is:

    // case class Message(senderId: Id[UserTable],
    //                    content: String,
    //                    ts: DateTime,
    //                    roomId: Option[Id[RoomTable]] = None,
    //                    toId: Option[Id[UserTable]] = None,
    //                    id: Id[MessageTable] = Id(0L))

    // To provide a GetResult[Message] we need all the types inside the Message to have GetResult instances.
    // We've already tackled DateTime. That leaves Id[MessageTable], Id[UserTable], Option[Id[UserTable],
    // and Option[Id[RoomTable].

    // Dealing with the two non-option IDs is straight-forward:

    implicit val GetUserId = GetResult(r => Id[UserTable](r.nextLong))
    implicit val GetMessageId = GetResult(r => Id[MessageTable](r.nextLong))

    // For the optional ones we need to use nextLongOption and then map to the right type:

    implicit val GetOptionalUserId = GetResult(r => r.nextLongOption.map(id => Id[UserTable](id)))
    implicit val GetOptionalRoomId = GetResult(r => r.nextLongOption.map(id => Id[RoomTable](id)))

    // With all the individual columns mapped we can pull them into a GetResult for Message. There are
    // two helper methods which make it easier to construct these instances:
    //   <<  for calling the appropriate nextXXX method
    //  <<?  when the value is optional

    // The following works as we've provided implicits for the components of the case class. As
    // the types of the fields are known, << and <<? simply expect the implicit GetResult[T]
    // for each type
    implicit val GetMessage = GetResult(r =>
      Message(senderId = r.<<,
        content = r.<<,
        ts = r.<<,
        id = r.<<,
        roomId = r.<<?,
        toId = r.<<?))

    val results: List[Message] =
      sql""" select * from "message" """.as[Message].list

    results.foreach(result => println(result))
  }
}