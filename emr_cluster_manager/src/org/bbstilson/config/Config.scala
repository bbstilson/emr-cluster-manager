package org.bbstilson.config

case class Config(
  clusterName: String,
  masterInstanceType: String,
  driverMemory: String,
  workerInstanceType: String,
  numWorkers: Int,
  steps: List[Step],
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
       |  steps: $steps,
       |  bootstrapAction: $bootstrapAction,
       |  logUri: $logUri,
       |  tags: $tags
       |)
    """.stripMargin
  }
}
