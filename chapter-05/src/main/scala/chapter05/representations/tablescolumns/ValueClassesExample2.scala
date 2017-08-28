package chapter05.representations.tablescolumns

import java.sql.Timestamp

import chapter05.framework.Profile
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import slick.dbio._
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ChatSchema2 {

  // A primary key type for each of our tables:
  object PKs {
    import slick.lifted.MappedTo
    final case class PK[A](value: Long) extends AnyVal with MappedTo[Long]
  }

  trait Tables {
    this: Profile =>

    import PKs._
    import profile.api._

    // DateTime <-> Timestamp mapping
    implicit val jodaDateTimeType =
      MappedColumnType.base[DateTime, Timestamp](
        dt => new Timestamp(dt.getMillis),
        ts => new DateTime(ts.getTime, UTC))

    // User table using UserPK as a primary key type
    case class User(name: String, id:PK[UserTable] = PK[UserTable](0L))

    class UserTable(tag: Tag) extends Table[User](tag, "user") {
      def id = column[PK[UserTable]]("id", O.PrimaryKey, O.AutoInc)

      def name = column[String]("name")

      def * = (name, id).mapTo[User]
    }

    lazy val users = TableQuery[UserTable]
    lazy val insertUser = users returning users.map(_.id)

    // Message table using a MessagePK as a primary key,
    // and referencing UserPK as a foreign key.
    case class Message(
                        senderId: PK[UserTable],
                        content: String,
                        ts: DateTime,
                        id: PK[MessageTable] = PK[MessageTable](0L))

    class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
      def id = column[PK[MessageTable]]("id", O.PrimaryKey, O.AutoInc)

      def senderId = column[PK[UserTable]]("sender")

      def content = column[String]("content")

      def ts = column[DateTime]("ts")

      def * = (senderId, content, ts, id).mapTo[Message]

      def sender = foreignKey("sender_fk", senderId, users)(_.id, onDelete = ForeignKeyAction.Cascade)
    }

    lazy val messages = TableQuery[MessageTable]
    lazy val ddl = users.schema ++ messages.schema

    def populate = {

      // Insert the conversation, which took place in Feb, 2001:
      val start = new DateTime(2001, 2, 17, 10, 22, 50)

      for {
        _ <- ddl.create
        halId <- insertUser += User("HAL")
        daveId <- insertUser += User("Dave")
        count <- messages ++= Seq(
          Message(daveId, "Hello, HAL. Do you read me, HAL?", start),
          Message(halId, "Affirmative, Dave. I read you.", start plusSeconds 2),
          Message(daveId, "Open the pod bay doors, HAL.", start plusSeconds 4),
          Message(halId, "I'm sorry, Dave. I'm afraid I can't do that.", start plusSeconds 6))
      } yield count

    }
  }

  class Schema(val profile: slick.jdbc.JdbcProfile) extends Tables with Profile

}

object ValueClassesExample2 extends App {

  import ChatSchema2._
  import ChatSchema2.PKs.PK

  val schema = new Schema(slick.jdbc.H2Profile)

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  import schema._
  import profile.api._

  val db = Database.forConfig("chapter05")

  exec(populate)

  // Won't compile:
  // users.filter(_.id === 6L)

  val halId = PK[UserTable](1L)
  val halsMessages = for {
    id <- messages.filter(_.senderId === halId).map(_.id)
    // rubbish <- messages.filter(_.senderId === 3L)
  } yield id

  exec(halsMessages.result).foreach { println }

  println("\nMessages in the database:")
  println(exec(messages.result))

  println("\nUsers in the database:")
  println(exec(users.result))
}