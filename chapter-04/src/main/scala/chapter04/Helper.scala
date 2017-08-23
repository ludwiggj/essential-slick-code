package chapter04

import slick.jdbc.H2Profile.api._

object Helper {

  def messages = TableQuery[MessageTable]

  // Helper method for creating test data:
  def testData = Seq(
    Message("Dave", "Hello, HAL. Do you read me, HAL?"),
    Message("HAL", "Affirmative, Dave. I read you."),
    Message("Dave", "Open the pod bay doors, HAL."),
    Message("HAL", "I'm sorry, Dave. I'm afraid I can't do that."))

  def recreateAndPopulateDb = {
    println("\nRecreating and populating database...")
    messages.schema.drop.asTry andThen
      messages.schema.create andThen
      (messages ++= testData)
  }
}