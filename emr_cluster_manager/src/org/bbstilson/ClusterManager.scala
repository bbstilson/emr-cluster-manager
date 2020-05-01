package org.bbstilson

import software.amazon.awssdk.services.emr.EmrClient
import software.amazon.awssdk.services.emr.model.{
  Application,
  BootstrapActionConfig,
  ClusterState,
  Configuration,
  DescribeClusterRequest,
  HadoopJarStepConfig,
  InstanceGroupConfig,
  JobFlowInstancesConfig,
  RunJobFlowRequest,
  RunJobFlowResponse,
  ScriptBootstrapActionConfig,
  StepConfig,
  Tag
}
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._
import java.util.concurrent._
import scala.concurrent.duration._

class ClusterManager(config: Config) extends LazyLogging {

  import ClusterManager._

  private[this] val client = EmrClient.create()

  val jobs: List[(String, List[String], String)] = {
    config.mainClasses.zip(config.mainClassArgs.zip(config.jarPaths)).map {
      case (mainClass, (mainClassArgs, jarPath)) => (mainClass, mainClassArgs, jarPath)
    }
  }

  private[this] val steps: List[StepConfig] = jobs.map {
    case (mainClass, mainClassArgs, jarPath) =>
      val args = List(
        "spark-submit",
        "--deploy-mode",
        "cluster",
        "--driver-memory",
        config.driverMemory,
        "--class",
        mainClass,
        jarPath
      ) ++ mainClassArgs

      val hadoopJarStep = HadoopJarStepConfig
        .builder()
        .jar("command-runner.jar")
        .args(args.asJava)
        .build()

      StepConfig
        .builder()
        .name(mainClass)
        .actionOnFailure(TERMINATE_CLUSTER)
        .hadoopJarStep(hadoopJarStep)
        .build()
  }

  private[this] val instances: JobFlowInstancesConfig = {
    val masterNodes = InstanceGroupConfig
      .builder()
      .name(MASTER_NODES)
      .market(ON_DEMAND)
      .instanceType(config.masterInstanceType)
      .instanceRole(MASTER)
      .instanceCount(1)
      .build()

    val workerNodes = InstanceGroupConfig
      .builder()
      .name(WORKER_NODES)
      .market(SPOT)
      .instanceRole(CORE)
      .instanceType(config.workerInstanceType)
      .instanceCount(config.numWorkers)
      .build()

    JobFlowInstancesConfig
      .builder()
      .instanceGroups(List(masterNodes, workerNodes).asJava)
      .ec2KeyName(Instances.DEV_KEY_PAIR)
      .ec2SubnetId(Instances.DEFAULT_SUBNET)
      .additionalMasterSecurityGroups(Instances.DEFAULT_SG)
      .additionalSlaveSecurityGroups(Instances.DEFAULT_SG)
      // Specifying a script will force shutdown when completed
      .keepJobFlowAliveWhenNoSteps(steps.isEmpty)
      .build()
  }

  private[this] val bootstrapAction: Option[BootstrapActionConfig] = config.bootstrap
    .map { s3Path =>
      val bootstrapActionConfig = ScriptBootstrapActionConfig
        .builder()
        .path(s3Path)
        .build()

      BootstrapActionConfig
        .builder()
        .name(s3Path)
        .scriptBootstrapAction(bootstrapActionConfig)
        .build()
    }

  private[this] val jobFlowReq = {
    val tags = config.tags.map { tag => Tag.builder().key(tag.key).value(tag.value).build() }
    RunJobFlowRequest
      .builder()
      .name(config.clusterName)
      .releaseLabel(CLUSTER_RELEASE)
      .applications(CLUSTER_APPLICATIONS)
      .configurations(CLUSTER_CONFIGURATIONS)
      .logUri(config.logUri)
      .instances(instances)
      .steps(steps.asJava)
      .bootstrapActions(List(bootstrapAction).flatten.asJava)
      .visibleToAllUsers(true)
      .jobFlowRole(JOB_FLOW_ROLE)
      .serviceRole(SERVICE_ROLE)
      .tags(tags.asJava)
      .build()
  }

  // Runs the job and returns the cluster id.
  def run(): String = client.runJobFlow(jobFlowReq).jobFlowId()

  def runAndWait(): Unit = {
    val clusterId = run()
    logger.info(s"Waiting for cluster $clusterId to finish.")
    waitForJobToComplete(clusterId)
    logger.info(s"Cluster($clusterId) has finished.")
  }

  private def waitForJobToComplete(clusterId: String): Unit = {
    val describeReq = DescribeClusterRequest.builder().clusterId(clusterId).build()

    var state: ClusterState = ClusterState.STARTING
    var waiting = true
    while (waiting) {
      state = client.describeCluster(describeReq).cluster.status.state
      if (isWaitState(state)) {
        logger.info(s"Cluster state: $state. Checking again in 1 minute.")
        Thread.sleep(1.minutes.toMillis)
      } else {
        waiting = false
        logger.info(s"Final cluster state: $state")
      }
    }
  }

  private def isWaitState(state: ClusterState): Boolean = {
    state == ClusterState.WAITING ||
    state == ClusterState.BOOTSTRAPPING ||
    state == ClusterState.RUNNING ||
    state == ClusterState.STARTING
  }
}

object ClusterManager {
  val CLUSTER_RELEASE = "emr-6.0.0"

  val TERMINATE_CLUSTER = "TERMINATE_CLUSTER"
  val ON_DEMAND = "ON_DEMAND"
  val MASTER = "MASTER"
  val SPOT = "SPOT"
  val CORE = "CORE"

  val MASTER_NODES = "Master Nodes"
  val WORKER_NODES = "Worker Nodes"

  val JOB_FLOW_ROLE = "EMR_EC2_DefaultRole"
  val SERVICE_ROLE = "EMR_DefaultRole"

  object Instances {
    val DEFAULT_SG = "sg-bb7c52de"
    val DEFAULT_SUBNET = "subnet-d841cabf"
    val DEV_KEY_PAIR = "s2dev"
  }

  val LIVY_CONF: Configuration = Configuration
    .builder()
    .classification("livy-conf")
    .properties(
      Map(
        "livy.server.session.timeout-check" -> "false"
      ).asJava
    )
    .build()

  // Make Glue tables available on the cluster
  val GLUE_CONF: Configuration = Configuration
    .builder()
    .classification("spark-hive-site")
    .properties(
      Map(
        "hive.metastore.client.factory.class" -> "com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory"
      ).asJava
    )
    .build()

  val CLUSTER_CONFIGURATIONS = List(LIVY_CONF, GLUE_CONF).asJava

  val CLUSTER_APPLICATIONS = List("Spark", "Hadoop", "Hive", "Livy").map { app =>
    Application.builder().name(app).build()
  }.asJava
}
