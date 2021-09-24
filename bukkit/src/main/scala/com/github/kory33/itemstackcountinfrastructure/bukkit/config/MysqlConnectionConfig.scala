package com.github.kory33.itemstackcountinfrastructure.bukkit.config

import cats.effect.kernel.Async
import doobie.util.transactor.Transactor

case class MysqlConnectionConfig(mysqlUrl: String, username: String, password: String) {

  import cats.implicits.given

  def transactor[F[_]: Async]: Transactor[F] =
    Transactor.fromDriverManager[F](
      "com.mysql.cj.jdbc.Driver",
      s"jdbc:mysql://${mysqlUrl}",
      username,
      password
    )

}
