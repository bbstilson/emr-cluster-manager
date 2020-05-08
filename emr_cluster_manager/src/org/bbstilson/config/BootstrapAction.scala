package org.bbstilson.config

sealed trait BootstrapAction {
  def name: String
  def scriptPath: String
  def args: List[String]
}

case class DatadogBootstrap(
  name: String,
  scriptPath: String,
  setupScriptPath: String,
  apiKey: String,
  clusterName: String,
  stageEnv: String
) extends BootstrapAction {
  def args = List(apiKey, clusterName, stageEnv, setupScriptPath)
}
