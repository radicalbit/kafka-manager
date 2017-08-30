/**
 * Copyright 2015 Yahoo Inc. Licensed under the Apache License, Version 2.0
 * See accompanying LICENSE file.
 */

package loader

import controllers.{BasicAuthenticationFilter, KafkaManagerContext}
import features.ApplicationFeatures
import models.navigation.Menus
import play.api.ApplicationLoader.Context
import play.api.db.evolutions.{DynamicEvolutions, EvolutionsComponents}
import play.api.db.slick.evolutions.SlickEvolutionsComponents
import play.api.i18n.I18nComponents
import play.api.routing.Router
import play.api.{ApplicationLoader, BuiltInComponentsFromContext}
import router.Routes
import play.api.db.slick.{DbName, SlickComponents}
import slick.driver.JdbcProfile


/**
 * Created by hiral on 12/2/15.
 */
class KafkaManagerLoader extends ApplicationLoader {
  def load(context: Context) = {
    new ApplicationComponents(context).application
  }
}

class ApplicationComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with I18nComponents with DatabaseComponents {
  private[this] implicit val applicationFeatures = ApplicationFeatures.getApplicationFeatures(context.initialConfiguration.underlying)
  private[this] implicit val menus = new Menus
  private[this] val kafkaManagerContext = new KafkaManagerContext(applicationLifecycle, context.initialConfiguration)
  private[this] lazy val applicationC = new controllers.Application(messagesApi, kafkaManagerContext)
  private[this] lazy val clusterC = new controllers.Cluster(messagesApi, kafkaManagerContext)
  private[this] lazy val topicC = new controllers.Topic(messagesApi, kafkaManagerContext)
  private[this] lazy val logKafkaC = new controllers.Logkafka(messagesApi, kafkaManagerContext)
  private[this] lazy val consumerC = new controllers.Consumer(messagesApi, kafkaManagerContext)
  private[this] lazy val preferredReplicaElectionC= new controllers.PreferredReplicaElection(messagesApi, kafkaManagerContext)
  private[this] lazy val reassignPartitionsC = new controllers.ReassignPartitions(messagesApi, kafkaManagerContext)
  private[this] lazy val kafkaStateCheckC = new controllers.api.KafkaStateCheck(messagesApi, kafkaManagerContext)
  private[this] lazy val assetsC = new controllers.Assets(httpErrorHandler)
  private[this] lazy val webJarsAssetsC = new controllers.WebJarAssets(httpErrorHandler, context.initialConfiguration, context.environment)
  private[this] lazy val apiHealthC = new controllers.ApiHealth(messagesApi)

  override lazy val httpFilters = Seq(BasicAuthenticationFilter(context.initialConfiguration))

  override val router: Router = new Routes(
    httpErrorHandler,
    applicationC,
    clusterC,
    topicC,
    logKafkaC,
    consumerC,
    preferredReplicaElectionC,
    reassignPartitionsC,
    kafkaStateCheckC,
    assetsC,
    webJarsAssetsC,
    apiHealthC
  ).withPrefix(context.initialConfiguration.getString("play.http.context").orNull)
}

trait DatabaseComponents extends SlickComponents with SlickEvolutionsComponents with EvolutionsComponents {

  lazy val dbConf = api.dbConfig[JdbcProfile](DbName("default"))

  val dynamicEvolutions: DynamicEvolutions = new DynamicEvolutions
  private[this] def init() = {
    applicationEvolutions
  }
  init()
}

