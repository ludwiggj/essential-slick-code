package chapter05.framework

import slick.jdbc.JdbcProfile

trait Profile {
  val profile: JdbcProfile
}