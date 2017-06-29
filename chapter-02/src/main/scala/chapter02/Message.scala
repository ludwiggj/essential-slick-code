package chapter02

// Case class representing a row in our table:
final case class Message(
                          sender: String,
                          content: String,
                          id: Long = 0L)
