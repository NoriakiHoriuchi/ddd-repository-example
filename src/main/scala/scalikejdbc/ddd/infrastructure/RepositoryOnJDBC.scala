package scalikejdbc.ddd.infrastructure

import scala.util.Try
import scalikejdbc._, SQLInterpolation._

/**
 * TODO English doc
 * JDBC用リポジトリのための骨格実装。
 */
abstract class RepositoryOnJDBC[ID <: Identifier[_], E <: Entity[ID]]
    extends Repository[ID, E] with SQLSyntaxSupport[E] {

  /**
   * `WrappedResultSet`をエンティティに変換する。
   *
   * @param resultSet `WrappedResultSet`
   * @return エンティティ
   */
  protected def convertResultSetToEntity(resultSet: WrappedResultSet): E

  /**
   * エンティティをINSERT SQLで利用される、値の列に変換する。
   *
   * `columnNames`の順序に対応する必要がある。
   *
   * @param entity エンティティ
   * @return 値の列。
   */
  protected def convertEntityToValues(entity: E): Seq[Any]

  /**
   * エンティティをUPDATE SQLで利用される、キーと値のマップに変換する。
   *
   * @param entity エンティティ
   * @return キーと値のマップ
   */
  protected def convertEntityToKeyValues(entity: E): Map[String, Any] =
    columnNames.zip(convertEntityToValues(entity)).toMap

  protected def getDBSession(ctx: EntityIOContext): DBSession = ctx match {
    case EntityIOContextOnJDBC(dbSession) => dbSession
    case _ => throw new IllegalArgumentException(s"ctx is $ctx")
  }

  def containsByIdentifier(identifier: ID)(implicit ctx: EntityIOContext): Try[Boolean] = Try {
    implicit val dbSession = getDBSession(ctx)
    sql"select count(id) from $table where id = ?".
      bind(identifier.value.toString).
      map(_.int(1) == 1).single().apply().
      getOrElse(false)
  }

  def resolveEntity(identifier: ID)(implicit ctx: EntityIOContext): Try[E] = Try {
    implicit val dbSession = getDBSession(ctx)
    sql"select * from $table where id = ?".
      bind(identifier.value.toString).
      map(convertResultSetToEntity).single().apply().
      getOrElse(throw EntityNotFoundException(identifier))
  }

  def storeEntity(entity: E)(implicit ctx: EntityIOContext): Try[(This, E)] = Try {
    implicit val dbSession = getDBSession(ctx)
    def update(entity: E) = {
      val usb = new UpdateSQLBuilder(sqls"update $table")
      val kv = convertEntityToKeyValues(entity).map {
        case (k, v) => (column.column(k), v)
      }
      usb.set(kv.toList: _*).toSQL.update().apply()
    }
    def insert(entity: E) = {
      val isb = new InsertSQLBuilder(sqls"insert into $table")
      val columns = columnNames.map(column.column)
      if (entity.id.isDefined) {
        isb.columns(columns.toList: _*).values(convertEntityToValues(entity): _*).toSQL.update().apply()
      } else {
        isb.columns(columns.toList.tail: _*).values(convertEntityToValues(entity): _*).toSQL.update().apply()
      }
    }
    if (entity.id.isDefined && update(entity) > 0) {
      (this.asInstanceOf[This], entity)
    } else {
      if (insert(entity) > 0) {
        (this.asInstanceOf[This], entity)
      } else {
        throw RepositoryIOException("store error")
      }
    }
  }

  def deleteByIdentifier(identifier: ID)(implicit ctx: EntityIOContext): Try[(This, E)] = {
    implicit val dbSession = getDBSession(ctx)
    resolveEntity(identifier).map {
      entity =>
        if (sql"delete from $table where id = ?".bind(identifier.value.toString).update().apply() > 0) {
          (this.asInstanceOf[This], entity)
        } else {
          throw RepositoryIOException("delete error")
        }
    }
  }

}
