package dao


import domain.{FileToken, MyFileInfo}
import doobie._
import doobie.implicits.toSqlInterpolator

trait FileInfoSql {
  def insertSql(fileInfo: MyFileInfo): Update0

  def findByTokenSql(token: FileToken): Query0[MyFileInfo]
}

object FileInfoSql {
  private final class FileInfoSqlImpl extends FileInfoSql {
    def insertSql(fileInfo: MyFileInfo): Update0 =
      sql"insert into file_info (file_extension, token) values (${fileInfo.fileExtension.value},${fileInfo.token.value})".update

    def findByTokenSql(token: FileToken): Query0[MyFileInfo] =
      sql"select * from file_info where token=${token.value}".query[MyFileInfo]
  }

  def make(): FileInfoSql = new FileInfoSqlImpl
}
