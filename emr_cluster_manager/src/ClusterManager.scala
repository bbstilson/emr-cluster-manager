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

class ClusterManager(
  name: String,
  masterInstanceType: String,
  workerInstanceType: String,
  numWorkers: Int,
  s3Prefix: String,
  bootstrap: Option[String],
  jarPaths: List[String]
) extends LazyLogging {

  import ClusterManager._

  private[this] val client = EmrClient.create()

  private[this] val steps: List[StepConfig] = jarPaths.zipWithIndex.map {
    case (jarPath, index) =>
      val hadoopJarStep = HadoopJarStepConfig
        .builder()
        .jar("command-runner.jar")
        .args(
          List(
            "spark-submit",
            "--deploy-mode",
            "cluster",
            "--driver-memory",
            "10G",
            "--class",
            "org.allenai.training_data.Main",
            s"s3://$s3Prefix/$jarPath",
            "--clickDataPath",
            "s3://ai2-s2-brandons/s2_search_service/training_data/historical_serp_click_events*",
            "--showDataPath",
            "s3://ai2-s2-brandons/s2_search_service/training_data/historical_serp_show_events*",
            "--outputLocation",
            "s3://ai2-s2-brandons/s2_search_service/training_data/historical_serp_spark_output/"
          ).asJava
        )
        .build()

      StepConfig
        .builder()
        .name(s"Spark Script Step $index")
        .actionOnFailure(TERMINATE_CLUSTER)
        .hadoopJarStep(hadoopJarStep)
        .build()
  }

  private[this] val instances: JobFlowInstancesConfig = {
    val masterNodes = InstanceGroupConfig
      .builder()
      .name(MASTER_NODES)
      .market(ON_DEMAND)
      .instanceType(masterInstanceType)
      .instanceRole(MASTER)
      .instanceCount(1)
      .build()

    val workerNodes = InstanceGroupConfig
      .builder()
      .name(WORKER_NODES)
      .market(SPOT)
      .instanceRole(CORE)
      .instanceType(workerInstanceType)
      .instanceCount(numWorkers)
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

  private[this] val bootstrapAction: Option[BootstrapActionConfig] = bootstrap.map { s3Path =>
    val bootstrapActionConfig = ScriptBootstrapActionConfig
      .builder()
      .path(s3Path)
      .build()

    BootstrapActionConfig
      .builder()
      .name(s"$s3Prefix/$s3Path")
      .scriptBootstrapAction(bootstrapActionConfig)
      .build()
  }

  private[this] val jobFlowReq = RunJobFlowRequest
    .builder()
    .name(name)
    .releaseLabel(CLUSTER_RELEASE)
    .applications(CLUSTER_APPLICATIONS)
    .configurations(CLUSTER_CONFIGURATIONS)
    .logUri("s3://ai2-s2-brandons/s2_search_service/training_data/spark_logs")
    .instances(instances)
    .steps(steps.asJava)
    .bootstrapActions(List(bootstrapAction).flatten.asJava)
    .visibleToAllUsers(true)
    .jobFlowRole(JOB_FLOW_ROLE)
    .serviceRole(SERVICE_ROLE)
    .build()

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
  val TERMINATE_CLUSTER = "TERMINATE_CLUSTER"
  val ON_DEMAND = "ON_DEMAND"
  val MASTER = "MASTER"
  val SPOT = "SPOT"
  val CORE = "CORE"
  val MASTER_NODES = "Master Nodes"
  val WORKER_NODES = "Worker Nodes"
  val JOB_FLOW_ROLE = "EMR_EC2_DefaultRole"
  val SERVICE_ROLE = "EMR_DefaultRole"
  val CLUSTER_RELEASE = "emr-6.0.0"

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

  val CLUSTER_TAGS = List(
    Tag.builder().key("Project").value("S2").build(),
    Tag.builder().key("Contact").value(System.getProperty("user.name")).build(),
    Tag.builder().key("Application").value("CAR").build()
  ).asJava
}
