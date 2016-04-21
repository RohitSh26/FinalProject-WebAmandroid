package com.actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern._
import com.typesafe.config.ConfigFactory
import org.sireum.amandroid.concurrent._
import org.sireum.amandroid.security.TaintAnalysisModules
import org.sireum.util._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by rohitsharma on 4/5/16.
  */
class MyPTAActor(string: String) extends Actor {


  implicit val actorSystem = ActorSystem("AmandroidTestApplication", ConfigFactory.load)
  lazy val supervisorActor = actorSystem.actorOf(Props[AmandroidSupervisorActor], name = "AmandroidSupervisorActor")

  implicit val timeout = Timeout(10.seconds)

  import actorSystem.dispatcher


  println("String is: " + string)


  ////create a method which will return the data {PointsToAnalysis} from the supervisor actor
  def getPointToAnalysisData(string: String): Future[Seq[Object]] = {


    val path = "/Users/rohitsharma/work/source/fcapps/" + string
    println("getPointToAnalysisData - PATH: " + path)

    val fileUris = FileUtil.listFiles(FileUtil.toUri(path), ".apk", true)
    println("fileUris: " + fileUris)
    val outputUri = FileUtil.toUri("/Users/rohitsharma/Documents/activator-1.3.7-minimal/web-amandroid/public")

    val futures = fileUris map {
      fileUri =>
        (supervisorActor.ask(AnalysisSpec(fileUri, outputUri, None, true, true))(600 minutes)).mapTo[PointsToAnalysisResult].recover{
          case ex: Exception =>
            PointsToAnalysisFailResult(fileUri, ex)
        }
    }
    val fseq = Future.sequence(futures)
    val seFutures: MSet[Future[SecurityEngineResult]] = msetEmpty
    Await.result(fseq, Duration.Inf).foreach {
      dr =>
        dr match {
          case ptar: PointsToAnalysisResult with Success =>
            seFutures += (supervisorActor.ask(SecurityEngineData(ptar, TaintAnalysisSpec(TaintAnalysisModules.DATA_LEAKAGE)))(10 minutes)).mapTo[SecurityEngineResult].recover{
              case ex: Exception =>
                SecurityEngineFailResult(ptar.fileUri, ex)
            }
          case _ =>
        }
    }
    val sefseq = Future.sequence(seFutures)
    Await.result(sefseq, Duration.Inf).foreach {
      sr => println(sr)
    }


    fseq
  }

  override def receive = {

    case _ => sender ! getPointToAnalysisData(string)

  }
}


object PointToAnalysisObject