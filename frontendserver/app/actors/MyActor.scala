package actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.sireum.amandroid.concurrent._
import org.sireum.util.FileUtil
import scala.concurrent.duration._
import scala.concurrent.Future


/**
  * Created by rohitsharma on 4/2/16.
  */
class MyActor extends Actor {

  implicit lazy val actorSystem = ActorSystem("AmandroidTestApplication", ConfigFactory.load)
  lazy val supervisorActor = actorSystem.actorOf(Props[AmandroidSupervisorActor], name = "AmandroidSupervisorActor")
  implicit val timeout = Timeout(10.seconds)
  import actorSystem.dispatcher
  ////create a method which will return the data {PointsToAnalysis} from the supervisor actor
  def getPointToAnalysisData: Future[Seq[Object]] = {

    val fileUris =  FileUtil.listFiles(FileUtil.toUri("/Users/rohitsharma/work/source/fcapps"), ".apk", true)
    val outputUri = FileUtil.toUri("/Users/rohitsharma/work/output/fcapps")

    val futures = fileUris map {
      fileUri =>
        (supervisorActor.ask(AnalysisSpec(fileUri, outputUri, None, true, true))(6000 minutes)).mapTo[PointsToAnalysisResult].recover {
          case ex: Exception =>
            (fileUri, false)
        }

    }

    //val fseq = Future.sequence(futures)

    val fseq = Future.sequence(futures)

    fseq
  }


  def receive = {
    case PointToAnalysisObject => sender ! getPointToAnalysisData
  }
}


object PointToAnalysisObject
