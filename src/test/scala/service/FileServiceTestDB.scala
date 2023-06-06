package service

import cats.implicits.toFunctorOps
import dao.FileInfoSql
import domain.{FileToken, MyFileInfo}
import doobie.{ConnectionIO, Query0, Update0}
import doobie.implicits.toSqlInterpolator

object FileServiceTestDB {
  def cleanupTestTable(): ConnectionIO[Unit] = {
    sql"DROP TABLE file_info_test".update.run.void
  }

  def setupTestTable(): ConnectionIO[Unit] = {
    sql"""
      CREATE TABLE file_info_test (
        file_extension VARCHAR(10),
        token VARCHAR(256) PRIMARY KEY
      )
    """.update.run.void
  }

  private final class FileInfoSqlTestImpl extends FileInfoSql {
    def insertSql(fileInfo: MyFileInfo): Update0 =
      sql"insert into file_info_test (file_extension, token) values (${fileInfo.fileExtension.value},${fileInfo.token.value})".update

    def findByTokenSql(token: FileToken): Query0[MyFileInfo] =
      sql"select * from file_info_test where token=${token.value}".query[MyFileInfo]
  }

  def makeTest(): FileInfoSql = new FileInfoSqlTestImpl

}
