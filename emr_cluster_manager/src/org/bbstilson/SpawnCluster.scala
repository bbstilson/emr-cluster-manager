package org.bbstilson

import org.bbstilson.config._

import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.error._
import pureconfig.generic.auto._
import scopt.OptionParser

object SpawnCluster {

  case class Args(configFile: String = "")

  val ARG_SIZE_FAILURE = ConfigReaderFailures(
    CannotParse("Number of main classes, jar paths, and main class args must be the same.", None),
    Nil
  )

  def main(args: Array[String]): Unit = {
    val argParser = new OptionParser[Args](getClass.getSimpleName) {
      opt[String]("configFile")
        .required()
        .text("Path to the EMR configuration.")
        .action((cp, c) => c.copy(configFile = cp))
    }

    argParser.parse(args, Args()).foreach { parsed =>
      run(ConfigSource.resources(parsed.configFile).loadOrThrow[Config])
    }
  }

  def run(config: Config): Unit = {
    new ClusterManager(config).runAndWait()
  }
}
