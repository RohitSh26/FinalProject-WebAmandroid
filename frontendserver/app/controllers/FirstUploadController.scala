package controllers

import java.io.{File}
import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import play.api.libs.mailer.MailerClient
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import org.sireum.amandroid.concurrent.AmandroidSupervisorActor

import play.api.data._
import play.api.data.Forms._
import play.api.libs.mailer._

/**
  * Created by rohitsharma on 4/7/16.
  */
class FirstUploadController @Inject()(mailerClient: MailerClient)(ws: WSClient) extends Controller {

  var emailAddress = ""
  val _system = ActorSystem("AmandroidTestApplication", ConfigFactory.load)
  val supervisor = _system.actorOf(Props[AmandroidSupervisorActor], name = "AmandroidSupervisorActor")

  def upload = Action {
    Ok(views.html.upload("*"))
  }

  def colloquia = Action {
    Ok(views.html.colloquia("colloquia"))
  }


  val form = Form(
    "filename" -> text
  )

  def search = Action { implicit request =>

    val apkFileDir = form.bindFromRequest.get
    val dir = new File("/Users/rohitsharma/work/source/fcapps/" + apkFileDir)
    if (dir.exists()) {
      Redirect("/result/" + apkFileDir)
    } else {
      Ok(views.html.upload("File Not Found"))
    }
  }


  // upload and process the apk
  def uploadFile = Action(parse.multipartFormData) { request =>

    request.body.file("file").map { apk =>
      val apkFileName = apk.filename

      val fileType  = apk.contentType

      val apkFileDir = apkFileName.dropRight(4)

      val dir = new File("/Users/rohitsharma/work/source/fcapps/" + apkFileDir)
      if (dir.exists()) {
        Redirect("/result/" + apkFileDir)
      } else if(! (apkFileName takeRight(3) equals("apk"))) {
        Ok(views.html.upload("Invalid file type"))
      } else {
        val emailID = request.body.dataParts.get("email").get

        for (elem <- emailID) {
          emailAddress = elem
        }
        dir.mkdir();
        val apkDirPath = "/Users/rohitsharma/work/source/fcapps/" + apkFileDir + "/" + apkFileName
        apk.ref.moveTo(new File(apkDirPath))
        Redirect("/pta/" + apkFileDir)
      }

    }.getOrElse {
      Redirect("/upload").flashing(
        "error" -> "File Not Uploaded"
      )
    }

  }


  def pta(apkFileDir: String) = Action {

    val apkDirPath = new File("./public/" + apkFileDir)
    if (apkDirPath.exists()) {

      Redirect("/result/" + apkFileDir)

    } else {
      val result = ws.url("http://localhost:8080/PTA/" + apkFileDir).withRequestTimeout(5 second).get().map{
        response =>
          response.toString
      }



      println(result)

      Redirect("/ptaemail/" + apkFileDir)
    }

  }


//
//
//  def ptaJSON(apkFileDir: String) = Action.async {
//
//    val futureInt = scala.concurrent.Future {
//      androidUrlCollector(apkFileDir: String)
//    }
//    val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Sending Email with link...", 1.second)
//    Future.firstCompletedOf(Seq(futureInt, timeoutFuture)).map {
//      case i: Int => Ok("Got result: " + i)
//      case t: String => Redirect("/notification")
//    }
//  }
//
//
//  def androidUrlCollector(apkFileDir: String) = {
//
//
//  }

  /* */




  def ptawithemail(apkFileDir: String) = Action.async {

    val futureInt = scala.concurrent.Future {
      runAmandroid((apkFileDir: String))
    }
    val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Sending Email with link...", 1.second)
    Future.firstCompletedOf(Seq(futureInt, timeoutFuture)).map {
      case i: Int => Ok("Got result: " + i)
      case t: String => Redirect("/notification")
    }
  }

  def runAmandroid(apkFileDir: String): Int = {
    val value = 3
    println("value of dir:" + apkFileDir)
    val dir = new File("./public/" + apkFileDir + "/stage")
    while (!dir.exists()) {
      Thread.sleep(2000);
    }

    sendEmailToUser(apkFileDir, emailAddress, apkFileDir)

    value
  }

  def sendEmailToUser(message: String, to: String, apkFileName: String) = {

    println("sending...")
    val url = "http://localhost:9000/result/" + message
    val email = Email(
      apkFileName + " - Amandroid Result Ready",
      "Sharma Rohit <rohit.sharma1048@gmail.com>",
      Seq("<" + to + ">"),
      //adds attachment
      attachments = Seq(
        //adds json files as an attachment
        AttachmentFile("apk.json", new File("./public/" + message + "/stage/apk.json")),
        AttachmentFile("ptaresult.json", new File("./public/" + message + "/stage/ptaresult.json")),
        AttachmentFile("taintresult.json", new File("./public/" + message + "/stage/taintresult.json"))
      ),
      //sends text, HTML or both...
      bodyText = Some("A text message"),
      bodyHtml = Some(s"""<html><body><p><b>Please visit the link below to see amandroid results for $apkFileName</b><br><br> <code>$url</code></p></body></html>""")
    )
    mailerClient.send(email)

  }

  def notifyuser = Action {
    Ok(views.html.notifyuser("Email will be sent"))
  }


  def result(apkDir: String) = Action {

    Ok(views.html.result(apkDir))
  }


}
