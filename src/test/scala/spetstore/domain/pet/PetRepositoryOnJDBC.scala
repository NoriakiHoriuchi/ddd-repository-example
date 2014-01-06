package spetstore.domain.pet

import scalikejdbc.ddd.infrastructure.RepositoryOnJDBC
import scalikejdbc.WrappedResultSet
import java.util.UUID
import spetstore.domain.basic.SexType

private[pet] class PetRepositoryOnJDBC
    extends RepositoryOnJDBC[PetId, Pet] with PetRepository {

  override def tableName: String = "pet"

  override def columnNames: Seq[String] = Seq(
    "id",
    "pet_type_id",
    "sex_type",
    "name",
    "description",
    "price",
    "supplier_id"
  )

  protected def convertResultSetToEntity(resultSet: WrappedResultSet): Pet =
    Pet(
      id = PetId(UUID.fromString(resultSet.string("id"))),
      sexType = SexType(resultSet.int("sexType")),
      petTypeId = PetTypeId(UUID.fromString(resultSet.string("pet_type_id"))),
      name = resultSet.string("name"),
      description = resultSet.stringOpt("description"),
      price = resultSet.long("price"),
      supplierId = SupplierId(UUID.fromString(resultSet.string("supplier_id")))
    )

  protected def convertEntityToValues(entity: Pet): Seq[Any] = Seq(
    entity.id,
    entity.petTypeId.value.toString,
    entity.sexType.id,
    entity.name,
    entity.description,
    entity.price,
    entity.supplierId.value.toString
  )

}
