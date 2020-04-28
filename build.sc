import mill._, scalalib._

object emr_cluster_manager extends ScalaModule {
  def scalaVersion = "2.13.1"

  def mainClass = Some("org.bbstilson.SpawnCluster")

  lazy val AwsSdkVersion = "2.10.84"

  def ivyDeps = Agg(
    ivy"com.github.scopt::scopt:3.7.1",
    ivy"com.github.pureconfig::pureconfig:0.12.3",
    ivy"software.amazon.awssdk:bom:$AwsSdkVersion",
    ivy"software.amazon.awssdk:s3:$AwsSdkVersion",
    ivy"software.amazon.awssdk:emr:$AwsSdkVersion",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.2",
    ivy"ch.qos.logback.contrib:logback-jackson:0.1.5",
    ivy"ch.qos.logback.contrib:logback-json-classic:0.1.5"
  )
}
