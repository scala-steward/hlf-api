package de.upb.cs.uc4.hyperledger.connections.traits

import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.util
import java.util.concurrent.TimeoutException

import com.google.protobuf.ByteString
import de.upb.cs.uc4.hyperledger.exceptions.traits.{ HyperledgerExceptionTrait, TransactionExceptionTrait }
import de.upb.cs.uc4.hyperledger.exceptions.{ HyperledgerException, NetworkException, TransactionException }
import org.hyperledger.fabric.gateway.impl.{ ContractImpl, GatewayImpl, NetworkImpl, TransactionImpl }
import org.hyperledger.fabric.gateway.{ ContractException, GatewayRuntimeException, Transaction }
import org.hyperledger.fabric.protos.peer.ProposalPackage
import org.hyperledger.fabric.sdk._
import org.hyperledger.fabric.sdk.transaction.{ ProposalBuilder, TransactionContext }

import scala.jdk.CollectionConverters.MapHasAsJava

trait ConnectionTrait extends AutoCloseable {
  val contractName: String
  val contract: ContractImpl
  val gateway: GatewayImpl

  @throws[HyperledgerExceptionTrait]
  protected final def internalSubmitTransaction(transient: Boolean, transactionId: String, params: String*): Array[Byte] = {
    testParamsNull(transactionId, params: _*)
    try {
      if (transient) {
        var transMap: Map[String, Array[Byte]] = Map()
        var i = 0
        params.foreach(param => {
          transMap += i.toString -> param.toCharArray.map(_.toByte)
          i = i + 1
        })

        contract.createTransaction(transactionId).setTransient(transMap.asJava).submit()
      }
      else {
        contract.submitTransaction(transactionId, params: _*)
      }
    }
    catch {
      case ex: GatewayRuntimeException => throw NetworkException(innerException = ex)
      case ex: TimeoutException        => throw NetworkException(innerException = ex)
      case ex: Exception               => throw HyperledgerException(transactionId, ex)
    }
  }

  @throws[HyperledgerExceptionTrait]
  protected final def internalEvaluateTransaction(transactionId: String, params: String*): Array[Byte] = {
    testParamsNull(transactionId, params: _*)
    try {
      contract.evaluateTransaction(transactionId, params: _*)
    }
    catch {
      case ex: GatewayRuntimeException => throw NetworkException(innerException = ex)
      case ex: TimeoutException        => throw NetworkException(innerException = ex)
      case ex: Exception               => throw HyperledgerException(transactionId, ex)
    }
  }

  final def createUnsignedTransaction(transactionName: String, params: String*): (ProposalPackage.Proposal, String) = {
    val transaction: TransactionImpl = contract.createTransaction(transactionName).asInstanceOf[TransactionImpl]
    val request: TransactionProposalRequest = callPrivateMethod(transaction)(Symbol("newProposalRequest"))(params.toArray).asInstanceOf[TransactionProposalRequest]
    val channel = contract.getNetwork().getChannel()
    val context: TransactionContext = callPrivateMethod(channel)(Symbol("getTransactionContext"))(request).asInstanceOf[TransactionContext]
    val proposalBuilder: ProposalBuilder = ProposalBuilder.newBuilder()
    proposalBuilder.context(context)
    proposalBuilder.request(request)
    (proposalBuilder.build(), context.getTxID())
  }

  @throws[HyperledgerExceptionTrait]
  final def submitSignedTransaction(proposal: ProposalPackage.Proposal, signature: ByteString, transactionName: String, transactionId: String, params: String*): Array[Byte] = {

    val signedProposalBuilder: ProposalPackage.SignedProposal.Builder = ProposalPackage.SignedProposal.newBuilder
    val signedProposal: ProposalPackage.SignedProposal = signedProposalBuilder.setProposalBytes(proposal.toByteString).setSignature(signature).build
    val transaction: TransactionImpl = contract.createTransaction(transactionName).asInstanceOf[TransactionImpl]
    val request: TransactionProposalRequest = callPrivateMethod(transaction)(Symbol("newProposalRequest"))(params.toArray).asInstanceOf[TransactionProposalRequest]
    val context: TransactionContext = callPrivateMethod(contract.getNetwork.getChannel)(Symbol("getTransactionContext"))(request).asInstanceOf[TransactionContext]
    setPrivateField(context)(Symbol("txID"))(transactionId)
    context.verify(request.doVerify)
    context.setProposalWaitTime(request.getProposalWaitTime)

    setPrivateField(transaction)(Symbol("transactionContext"))(context)

    val peers: util.Collection[Peer] = callPrivateMethod(contract.getNetwork().getChannel())(Symbol("getEndorsingPeers"))().asInstanceOf[util.Collection[Peer]]

    val proposalResponses: util.Collection[ProposalResponse] = callPrivateMethod(contract.getNetwork().getChannel())(Symbol("sendProposalToPeers"))(peers, signedProposal, context).asInstanceOf[util.Collection[ProposalResponse]]
    val validResponses = callPrivateMethod(transaction)(Symbol("validatePeerResponses"))(proposalResponses).asInstanceOf[util.Collection[ProposalResponse]]

    try callPrivateMethod(transaction)(Symbol("commitTransaction"))(validResponses).asInstanceOf[Array[Byte]]
    catch {
      case e: ContractException =>
        e.setProposalResponses(proposalResponses)
        throw e
    }
    //} catch {
    //  case e@(_: InvalidArgumentException | _: ProposalException | _: ServiceDiscoveryException) =>
    //   throw new GatewayRuntimeException(e)
    //}
  }

  class PrivateMethodCaller(x: AnyRef, methodName: String) {
    def apply(_args: Any*): Any = {
      val args = _args.map(_.asInstanceOf[AnyRef])
      def _parents: LazyList[Class[_]] = LazyList(x.getClass) #::: _parents.map(_.getSuperclass)
      val parents = _parents.takeWhile(_ != null).toList
      val methods = parents.flatMap(_.getDeclaredMethods)
      val method = methods.find(_.getName == methodName).getOrElse(throw new IllegalArgumentException("Method " + methodName + " not found"))
      method.setAccessible(true)
      method.invoke(x, args: _*)
    }
  }

  class PrivateMethodExposer(x: AnyRef) {
    def apply(method: scala.Symbol): PrivateMethodCaller = new PrivateMethodCaller(x, method.name)
  }

  def callPrivateMethod(x: AnyRef): PrivateMethodExposer = new PrivateMethodExposer(x)

  class PrivateFieldCaller(x: AnyRef, fieldName: String) {
    def apply(_arg: Any): Any = {
      val arg = _arg.asInstanceOf[AnyRef]
      def _parents: LazyList[Class[_]] = LazyList(x.getClass) #::: _parents.map(_.getSuperclass)
      val parents = _parents.takeWhile(_ != null).toList
      val fields = parents.flatMap(_.getDeclaredFields)
      val field = fields.find(_.getName == fieldName).getOrElse(throw new IllegalArgumentException("Method " + fieldName + " not found"))
      field.setAccessible(true)
      field.set(x, arg)
    }
  }

  class PrivateFieldExposer(x: AnyRef) {
    def apply(field: scala.Symbol): PrivateFieldCaller = new PrivateFieldCaller(x, field.name)
  }

  def setPrivateField(x: AnyRef): PrivateFieldExposer = new PrivateFieldExposer(x)


  /** Since the chain returns bytes, we need to convert them to a readable Result.
    *
    * @param result Bytes containing a result from a chaincode transaction.
    * @return Result as a String.
    */
  protected final def convertTransactionResult(result: Array[Byte]): String = {
    new String(result, StandardCharsets.UTF_8)
  }

  /** Wraps the chaincode query result bytes.
    * Translates the byte-array to a string and throws an error if said string is not empty
    *
    * @param result input byte-array to translate
    * @return result as a string
    */
  @throws[TransactionExceptionTrait]
  protected final def wrapTransactionResult(transactionId: String, result: Array[Byte]): String = {
    val resultString = convertTransactionResult(result)
    if (containsError(resultString)) throw TransactionException(transactionId, resultString)
    else resultString
  }

  /** Evaluates whether a transactionResult contains a "detailedError" or a "genericError"
    *
    * @param result result of a chaincode transaction
    * @return true if the result contains error information conforming to API-standards
    */
  private def containsError(result: String): Boolean = {
    result.contains("{\"type\":") && result.contains("\"title\":")
  }

  final override def close(): Unit = this.gateway.close()

  /** Checks if the transaction params are null.
    *
    * @param transactionId transactionId causing the error.
    * @param params parameters to check
    * @throws TransactionException if a parameter is null.
    */
  @throws[TransactionExceptionTrait]
  private def testParamsNull(transactionId: String, params: String*): Unit = {
    params.foreach(param => if (param == null) throw TransactionException.CreateUnknownException(transactionId, "A parameter was null."))
  }
}
