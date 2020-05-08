package org.bbstilson.config

case class Config(
  clusterName: String,
  masterInstanceType: String,
  driverMemory: String,
  workerInstanceType: String,
  numWorkers: Int,
  jarPaths: List[String],
  mainClasses: List[String],
  mainClassArgs: List[List[String]],
  bootstrapAction: Option[BootstrapAction],
  logUri: String,
  tags: List[Tag]
) {

  override def toString(): String = {
    s"""
       |Config(
       |  clusterName: $clusterName,
       |  masterInstanceType: $masterInstanceType,
       |  driverMemory: $driverMemory,
       |  workerInstanceType: $workerInstanceType,
       |  numWorkers: $numWorkers,
       |  jarPaths: $jarPaths,
       |  mainClasses: $mainClasses,
       |  mainClassArgs: $mainClassArgs,
       |  bootstrapAction: $bootstrapAction,
       |  logUri: $logUri,
       |  tags: $tags
       |)
    """.stripMargin
  }
}
