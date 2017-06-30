package chapter03

import slick.jdbc.H2Profile.api._
import scala.concurrent.ExecutionContext.Implicits.global

object Helper {

  def messages = TableQuery[MessageTable]

  // Helper method for creating test data:
  def testData = Seq(
    Message("Dave", "Hello, HAL. Do you read me, HAL?"),
    Message("HAL", "Affirmative, Dave. I read you."),
    Message("Dave", "Open the pod bay doors, HAL."),
    Message("HAL", "I'm sorry, Dave. I'm afraid I can't do that."))

  //Drop table if it already exists, then create the table:
  def recreateDb: DBIOAction[Option[Int], NoStream, Effect.All] = {
    for {
      _ <- messages.schema.drop.asTry andThen messages.schema.create
      // Add some data:
      // This is an example of a batch insert
      count <- messages ++= testData
    } yield count
  }

  def repopulateDb = {
    messages ++= testData.filter(_.sender == "HAL")
  }
}