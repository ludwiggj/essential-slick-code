package chapter05.representations.rows.hlists

import chapter05.framework.Profile
import slick.collection.heterogeneous.HNil
import slick.collection.heterogeneous.syntax._
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

// Code relating to 5.2.3 "Heterogeneous Lists"

object HListSansCaseClass extends App {

  trait Tables {
    this: Profile =>

    import profile.api._

    type AttrHList = Long :: Long :: String :: Int :: String :: Int :: String :: Int ::
      String :: Int :: String :: Int :: String :: Int :: String :: Int :: String ::
      Int :: String :: Int :: String :: Int :: String :: Int :: String :: Int :: HNil

    final class AttrTable(tag: Tag) extends Table[AttrHList](tag, "attrs") {
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

      def productId = column[Long]("product_id")

      def name1 = column[String]("name1")

      def value1 = column[Int]("value1")

      def name2 = column[String]("name2")

      def value2 = column[Int]("value2")

      def name3 = column[String]("name3")

      def value3 = column[Int]("value3")

      def name4 = column[String]("name4")

      def value4 = column[Int]("value4")

      def name5 = column[String]("name5")

      def value5 = column[Int]("value5")

      def name6 = column[String]("name6")

      def value6 = column[Int]("value6")

      def name7 = column[String]("name7")

      def value7 = column[Int]("value7")

      def name8 = column[String]("name8")

      def value8 = column[Int]("value8")

      def name9 = column[String]("name9")

      def value9 = column[Int]("value9")

      def name10 = column[String]("name10")

      def value10 = column[Int]("value10")

      def name11 = column[String]("name11")

      def value11 = column[Int]("value11")

      def name12 = column[String]("name12")

      def value12 = column[Int]("value12")

      def * = id :: productId :: name1 :: value1 :: name2 :: value2 :: name3 :: value3 ::
        name4 :: value4 :: name5 :: value5 :: name6 :: value6 :: name7 :: value7 :: name8 ::
        value8 :: name9 :: value9 :: name10 :: value10 :: name11 :: value11 :: name12 ::
        value12 :: HNil
    }

    val attributes = TableQuery[AttrTable]
  }

  class Schema(val profile: JdbcProfile) extends Tables with Profile

  val schema = new Schema(slick.jdbc.H2Profile)

  import schema._
  import profile.api._

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  val db = Database.forConfig("chapter05")

  val program: DBIO[Seq[AttrHList]] = for {
    _ <- attributes.schema.create
    _ <- attributes += 0L :: 100L ::
    "name1" :: 1 :: "name2" :: 2 :: "name3" :: 3 ::
    "name4" :: 4 :: "name5" :: 5 :: "name6" :: 6 ::
    "name7" :: 7 :: "name8" :: 8 :: "name9" :: 9 ::
    "name10" :: 10 :: "name11" :: 11 :: "name12" :: 12 ::
    HNil
    rows <- attributes.filter(_.value1 === 1).result
  } yield rows

  val myAttrsSeq: Seq[AttrHList] = exec(program)

  val myAttrs = myAttrsSeq.head

  val id: Long = myAttrs.head
  val productId: Long = myAttrs.tail.head
  val name1: String = myAttrs(2).asInstanceOf[String]
  val value1 = myAttrs(3)

  println(s"id [${id}] productId [${productId}] name1 [${name1}] value1 [${value1}]")
}