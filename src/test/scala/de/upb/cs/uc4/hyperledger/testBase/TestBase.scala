package de.upb.cs.uc4.hyperledger.testBase

import java.nio.file.Path

import de.upb.cs.uc4.hyperledger.connections.cases.{ ConnectionCourses, ConnectionMatriculation }
import de.upb.cs.uc4.hyperledger.connections.traits.{ ConnectionCourseTrait, ConnectionMatriculationTrait }
import de.upb.cs.uc4.hyperledger.utilities.EnrollmentManager

class TestBase extends TestBaseTrait {
  private val testBase: TestBaseTrait = tryRetrieveEnvVar("Target") match {
    case "ProductionNetwork" => new TestBaseProductionNetwork
    case _                   => new TestBaseDevNetwork
  }
  override val networkDescriptionPath: Path = testBase.networkDescriptionPath
  override val minikubeIP: String = testBase.minikubeIP
  override val caURL: String = testBase.caURL
  override val tlsCert: Path = testBase.tlsCert
  override val username: String = testBase.username
  override val password: String = testBase.password
  override val organisationId: String = testBase.organisationId
  override val channel: String = testBase.channel
  override val chaincode: String = testBase.chaincode

  override def beforeAll(): Unit = {
    log("Begin test with testBase Name = " + testBase.getClass.getName)
    if (testBase.isInstanceOf[TestBaseProductionNetwork]) {
      log("Begin enrollment with: "
        + " " + caURL
        + " " + tlsCert
        + " " + walletPath
        + " " + username
        + " " + password
        + " " + organisationId)
      EnrollmentManager.enroll(caURL, tlsCert, walletPath, username, password, organisationId)
      log("Finished Enrollment")
    }
  }

  def initializeCourses(): ConnectionCourseTrait = new ConnectionCourses(testBase.username, testBase.channel, testBase.chaincode, testBase.walletPath, testBase.networkDescriptionPath)
  def initializeMatriculation(): ConnectionMatriculationTrait = ConnectionMatriculation(testBase.username, testBase.channel, testBase.chaincode, testBase.walletPath, testBase.networkDescriptionPath)

  private def tryRetrieveEnvVar(varName: String, fallBack: String = ""): String = {
    sys.env.contains(varName) match {
      case true => {
        val value = sys.env(varName)
        log("####### Retrieved variable: " + varName + " with value: " + value)
        value
      }
      case false => {
        log("####### Returned default fallback")
        fallBack
      }
    }
  }

  private def log(message: String): Unit = {
    println("[TestBase] :: " + message)
  }
}