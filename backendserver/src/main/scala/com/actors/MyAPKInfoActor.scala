package com.actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern._
import com.typesafe.config.ConfigFactory
import org.sireum.amandroid.concurrent.{AmandroidSupervisorActor, AnalysisSpec, ApkInfoCollectResult}
import org.sireum.util.FileUtil

import scala.concurrent.duration._
import scala.concurrent.Future

/**
  * Created by rohitsharma on 4/5/16.
  */
class MyAPKInfoActor(string: String) extends Actor{



  implicit val actorSystem = ActorSystem("AmandroidTestApplication", ConfigFactory.load)
  lazy val supervisorActor = actorSystem.actorOf(Props[AmandroidSupervisorActor], name = "AmandroidSupervisorActor")

  implicit val timeout = Timeout(10.seconds)
  import actorSystem.dispatcher


  println("String is: "+string)


  ////create a method which will return the data {PointsToAnalysis} from the supervisor actor
  def getPointToAnalysisData(string: String): Future[Seq[Object]] = {


    val path = "/Users/rohitsharma/work/source/fcapps/"+string
    println("getPointToAnalysisData - PATH: "+path)

    val fileUris = FileUtil.listFiles(FileUtil.toUri(path), ".apk", true)
    println("fileUris: "+fileUris)
    val outputUri = FileUtil.toUri("/Users/rohitsharma/Documents/activator-1.3.7-minimal/web-amandroid/public")

    val futures = fileUris map {
      fileUri =>
        (supervisorActor.ask(AnalysisSpec(fileUri, outputUri, None, true, true))(6000 minutes)).mapTo[ApkInfoCollectResult].recover {
          case ex: Exception =>
            (fileUri, false)
        }

    }


    val fseq = Future.sequence(futures)

    fseq
  }

  override def receive = {

    case _ => sender ! getPointToAnalysisData(string)

  }
}


object APKInfoObject