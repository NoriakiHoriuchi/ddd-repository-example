package spetstore.infrastructure

import scalikejdbc.ddd.infrastructure._
import org.specs2.mutable.Specification

class RepositoryOnMemorySpec extends Specification {

  case class PersonRepositoryOnMemory(entities: Map[PersonId, Person] = Map.empty)
      extends RepositoryOnMemory[PersonId, Person](entities) with PersonRepository {

    type This = PersonRepository

    protected def createInstance(entities: Map[PersonId, Person]): This =
      new PersonRepositoryOnMemory(entities)

  }

  "repository" should {
    implicit val ctx = EntityIOContextOnMemory
    "store a entity" in {
      val personId = PersonId()
      val person = Person(personId, "Junichi", "Kato")
      PersonRepositoryOnMemory().
        storeEntity(person) must beSuccessfulTry.like {
          case (PersonRepositoryOnMemory(entities), _) =>
            entities.contains(personId) must beTrue
        }
    }

    "contains a entity" in {
      val personId = PersonId()
      val person = Person(personId, "Junichi", "Kato")
      val entities = Map(personId -> person)
      entities.contains(personId) must beTrue
      PersonRepositoryOnMemory(entities).
        containsByIdentifier(personId) must beSuccessfulTry.like {
          case result =>
            result must beTrue
        }
    }

    "get a entity" in {
      val personId = PersonId()
      val person = Person(personId, "Junichi", "Kato")
      PersonRepositoryOnMemory(Map(personId -> person)).
        resolveEntity(personId) must beSuccessfulTry.like {
          case entity =>
            entity must_== person
        }
    }

    "delete a entity" in {
      val personId = PersonId()
      val person = Person(personId, "Junichi", "Kato")
      PersonRepositoryOnMemory(Map(personId -> person)).
        deleteByIdentifier(personId) must beSuccessfulTry.like {
          case (PersonRepositoryOnMemory(entities), _) =>
            entities.contains(personId) must beFalse
        }

    }

  }

}
