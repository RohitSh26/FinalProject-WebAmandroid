package com.example

import java.io.{File, FileOutputStream}

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.ask
import spray.routing.SimpleRoutingApp
import com.typesafe.config._

import scala.concurrent._
import scala.concurrent.duration._
import com.actors.{APKInfoObject, MyAPKInfoActor, MyPTAActor, PointToAnalysisObject}
import org.sireum.amandroid.concurrent.{AmandroidSupervisorActor, AnalysisSpec, PointsToAnalysisResult}
import org.sireum.util.FileUtil
import spray.http.MediaTypes._
import spray.http.{BodyPart, MediaTypes, MultipartFormData, StatusCodes}
import spray.json._
import DefaultJsonProtocol._

import scala.io.Source

object Boot extends App with SimpleRoutingApp {
  implicit val actorSystem = ActorSystem("AmandroidTestApplication", ConfigFactory.load)

  implicit val timeout = Timeout(1 second)
  implicit val dispatcher =  actorSystem.dispatcher


  //start a web server
  startServer(interface = "localhost", port = 8080) {
    
    //get service
    get {
      path("test") {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            com.example.html.upload("Bob", 42).toString
          }
        }
      }
    } ~
      get {
        path("pta"/Segment) { apkFileDir =>

          println(apkFileDir)

          lazy val myApkInfoActor = actorSystem.actorOf(Props(new MyAPKInfoActor(apkFileDir)))

          myApkInfoActor ? apkFileDir


          complete {
            "OK"
          }

        }
      }
      get {
        path("PTA"/Segment) { apkFileDir =>

          println(apkFileDir)

          lazy val myPTAActor = actorSystem.actorOf(Props(new MyPTAActor(apkFileDir)))

          myPTAActor ? apkFileDir


          complete {
            "OK"
          }

        }
      } ~
      get {
        path("pta1"/Segment) { apkFileDir =>

          val _system = ActorSystem("AmandroidTestApplication", ConfigFactory.load)
          val supervisor = _system.actorOf(Props[AmandroidSupervisorActor], name = "AmandroidSupervisorActor")
          val fileUris = FileUtil.listFiles(FileUtil.toUri("/Users/fgwei/Work/Source/fcapps"+apkFileDir), ".apk", true)
          val outputUri = FileUtil.toUri("/Users/rohitsharma/Documents/activator-1.3.7-minimal/web-amandroid/public")
          val futures = fileUris map {
            fileUri =>
              (supervisor.ask(AnalysisSpec(fileUri, outputUri, None, true, true))(6000 minutes)).mapTo[PointsToAnalysisResult].recover{
                case ex: Exception =>
                  (fileUri, false)
              }
          }
          val fseq = Future.sequence(futures)
          Await.result(fseq, Duration.Inf).foreach {
            dr =>
              println(dr)
          }
          complete {
            "OK"
          }

        }
      } ~
      get {
        path("welcome") {
          respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
            complete {
              com.example.html.welcome("*").toString
            }
          }
        }
      } ~
      post {
        path("processing") {
          respondWithMediaType(MediaTypes.`application/json`) {

            entity(as[MultipartFormData]) { formData =>

              val aFileDir = uploadAPK(formData)
              lazy val myApkInfoActor = actorSystem.actorOf(Props(new MyAPKInfoActor(aFileDir)))
              myApkInfoActor ? aFileDir

              //val newFile = Uploads(aFileUploaded.getAbsolutePath)
              println("in")

              complete {
                "ok"+aFileDir
              }

            }
          }
        }
      }
  }


  def uploadAPK(multipartFormData: MultipartFormData): String = {

    val file = multipartFormData.get("file")

    println("Body Part Name is: " + file.get.headers)

    //Body Part
    val fileBodyPart: BodyPart = file.get

    //get filename from BodyPart
    val aFileName = fileBodyPart.filename

    //Convert filename to string
    val aFileNameString = aFileName.get

    val aFileDir = aFileNameString.dropRight(4)

    println("aFileNameString: " + aFileDir)

    val fileBodyPartEntity = file.get.entity

    val fileByteArray = fileBodyPartEntity.data.toByteArray

    val aFileUploaded = new File(aFileNameString)

    val aFileOutputStream = new FileOutputStream(aFileUploaded)

    aFileOutputStream.write(fileByteArray)

    aFileDir

  }


}

//~
//      get {
//        path("apkinfo") {
//          lazy val myPTAActor = actorSystem.actorOf(Props(new MyPTAActor(apkFileDir)))
//
//          complete {
//            (myPtaActor ? PointToAnalysisObject)(5 minutes).mapTo[Future[Seq[Object]]]
//              .map(s => Await.result(s, Duration.Inf).foreach {
//                dr =>
//                  println(dr)
//              })
//            "OK"
//          }
//
//        }
//}