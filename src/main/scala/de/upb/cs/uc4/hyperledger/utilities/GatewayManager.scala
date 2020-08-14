package de.upb.cs.uc4.hyperledger.utilities

import java.nio.file.Path

import org.hyperledger.fabric.gateway.Gateway.Builder
import org.hyperledger.fabric.gateway.{Gateway, Wallet}

object GatewayManager {

  def createGateway(wallet_path : Path, network_config_path : Path, username : String): Gateway = {
    val wallet: Wallet = WalletManager.getWallet(wallet_path)

    // prepare Network Builder
    this.createGateway(wallet, network_config_path, username)
  }

  def createGateway(wallet : Wallet, network_config_path : Path, username : String): Gateway = {
    // prepare Network Builder
    val builder: Builder = this.getBuilder(wallet, network_config_path, username)
    builder.connect()
  }

  def getBuilder(wallet: Wallet, networkConfigPath: Path, name: String): Builder = {
    // load a CCP
    var builder = Gateway.createBuilder
    builder = builder.identity(wallet, name)
    builder = builder.networkConfig(networkConfigPath)

    builder
  }

  def disposeGateway(gateway: Gateway): Unit = {
    if (gateway != null) gateway.close()
  }
}
