package de.upb.cs.uc4.hyperledger.tests.contracts

import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionExaminationRegulationTrait
import de.upb.cs.uc4.hyperledger.testBase.TestBase
import de.upb.cs.uc4.hyperledger.testUtil.{ TestDataExaminationRegulation, TestHelper, TestHelperStrings }
import de.upb.cs.uc4.hyperledger.utilities.helper.Logger

class ExaminationRegulationErrorTests extends TestBase {

  var chaincodeConnection: ConnectionExaminationRegulationTrait = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    chaincodeConnection = initializeExaminationRegulation()
  }

  override def afterAll(): Unit = {
    chaincodeConnection.close()
    super.afterAll()
  }

  def validTestModule(id: String): String = TestDataExaminationRegulation.getModule(id, "testName")

  "The ScalaAPI for ExaminationRegulations" when {
    "invoked with addExaminationRegulation incorrectly " should {
      val testData: Seq[(String, Option[String], Option[Seq[String]], Boolean)] = Seq(
        ("throw an exception for ExaminationRegulations with empty Modules", Some("001"), Some(Seq()), true),
        ("throw an exception for ExaminationRegulations with empty Modules", Some("001"), Some(Seq()), false),
        ("throw an exception for ExaminationRegulations with null Modules", Some("001"), None, true),
        ("throw an exception for ExaminationRegulations with null Modules", Some("001"), None, false),
        ("throw an exception for ExaminationRegulations with empty Name", Some(""), Some(Seq(validTestModule("1"))), true),
        ("throw an exception for ExaminationRegulations with empty Name", Some(""), Some(Seq(validTestModule("1"))), false)
      )
      for ((testDescription: String, name: Option[String], modules: Option[Seq[String]], open: Boolean) <- testData) {
        s"$testDescription [${name.orNull}][${TestHelperStrings.nullableSeqToString(modules.orNull)}][$open]" in {
          Logger.info("Begin test: " + testDescription)
          val examinationRegulation: String = TestDataExaminationRegulation.validExaminationRegulation(name.orNull, modules.orNull, open)
          TestHelper.testTransactionException(
            "approveTransaction",
            () => chaincodeConnection.addExaminationRegulation(examinationRegulation)
          )
        }
      }
    }
    "invoked with getExaminationRegulations incorrectly " should {
      val testData: Seq[(String, String)] = Seq(
        /* these are allowed, empty names are ignored
        ("throw TransactionException for malformed examinationRegulationNamesList", "[001,]"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "[001, ]"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "[,001]"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "[,001,]"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "[,001, ]"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "[ ,001]"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "[ ,001,]"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "[ ,001, ]"),
        */
        ("throw TransactionException for malformed examinationRegulationNamesList", "[001"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "001]"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "001"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "(001)"),
        ("throw TransactionException for malformed examinationRegulationNamesList", "{001}")
      )
      for ((statement: String, namesString: String) <- testData) {
        s"$statement [$namesString]" in {
          Logger.info("Begin test: " + statement)
          TestHelper.testTransactionException("approveTransaction", () => chaincodeConnection.getExaminationRegulations(namesString))
        }
      }
    }
    "invoked with closeExaminationRegulation incorrectly " should {
      val testData: Seq[(String, String)] = Seq(
        ("throw TransactionException for not existing examinationRegulationName ", "010"),
        ("throw TransactionException for empty examinationRegulationName ", ""),
        ("throw TransactionException for null examinationRegulationName ", null),
      )
      for ((statement: String, name: String) <- testData) {
        s"$statement [$name]" in {
          Logger.info("Begin test: " + statement)
          TestHelper.testTransactionException("approveTransaction", () => chaincodeConnection.closeExaminationRegulation(name))
        }
      }
    }
  }
}