package chapter03

import slick.jdbc.H2Profile.api._

// Schema for the "message" table:
class MessageTable(tag: Tag) extends Table[Message](tag, "message") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def sender = column[String]("sender")

  def content = column[String]("content")

  def * = (sender, content, id).mapTo[Message]
}