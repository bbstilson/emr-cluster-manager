package org.bbstilson

import org.bbstilson.config._

import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.error._
import pureconfig.generic.auto._
import scopt.OptionParser

object SpawnCluster {

  case class Args(configFile: String = "")

  def main(args: Array[String]): Unit = {
    val argParser = new OptionParser[Args](getClass.getSimpleName) {
      opt[String]("configFile")
        .required()
        .text("Path to the EMR configuration.")
        .action((cp, c) => c.copy(configFile = cp))
    }

    argParser
      .parse(args, Args())
      .map(_.configFile)
      .foreach(run)
  }

  def run(configPath: String): Unit = {
    run(ConfigSource.resources(configPath).loadOrThrow[Config])
  }

  def run(config: Config): Unit = {
    new ClusterManager(config).runAndWait()
  }
}
