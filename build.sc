import $ivy.`io.github.davidgregory084::mill-tpolecat:0.1.3`

import mill._, scalalib._
import io.github.davidgregory084.TpolecatModule

object emr_cluster_manager extends TpolecatModule {
  def scalaVersion = "2.13.1"

  def mainClass = Some("org.bbstilson.SpawnCluster")

  lazy val AwsSdkVersion = "2.10.84"

  def ivyDeps = Agg(
    ivy"com.github.pureconfig::pureconfig:0.12.3",
    ivy"software.amazon.awssdk:bom:$AwsSdkVersion",
    ivy"software.amazon.awssdk:emr:$AwsSdkVersion",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.2",
    ivy"ch.qos.logback:logback-classic:1.2.3"
  )
}
