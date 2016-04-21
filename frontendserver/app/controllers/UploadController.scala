//package controllers
//
//
//import java.io.File
//import javax.inject.Inject
//
//import play.api.mvc.{Action, Controller, Request}
//import play.api.libs.mailer._
//
//import scala.concurrent.{Await, Future, Promise}
//import scala.io._
//import scala.sys.process._
//import scala.concurrent.duration._
//import model.UploadFormInfo
//import actors.{MyActor, PointToAnalysisObject}
//import org.apache.commons.mail.{EmailAttachment, SimpleEmail}
//import akka.actor._
//import akka.util.Timeout
//import akka.pattern._
//import com.typesafe.config.ConfigFactory
//import play.api.libs.ws.WSClient
//
//import scala.concurrent.ExecutionContext
///**
//  * Created by rohitsharma on 3/26/16.
//  */
//class UploadController @Inject() (mailerClient: MailerClient) (ws: WSClient) extends Controller {
//
//  implicit lazy val actorSystem = ActorSystem("AmandroidTestApplication", ConfigFactory.load)
//  import actorSystem.dispatcher
//  var emailAddress = ""
//
//  implicit val timeout = Timeout(10.seconds)
//
//
//
//  // displays the upload form to the user
//  def upload = Action {
//    Ok(views.html.upload("Hello"))
//  }
//
//  // upload and process the apk
//  def processing = Action(parse.multipartFormData) { request =>
//
//    request.body.file("file").map { apk =>
//
//      val emailID = request.body.dataParts.get("email").get.toString().mkString
//
//      println(emailID)
//
//      emailAddress = emailID
//
//      import java.io.File
//
//      val apkFileName = apk.filename
//
//
//      apk.ref.moveTo(new File(s"/Users/rohitsharma/work/source/fcapps/$apkFileName"))
//      val apkDir = apkFileName.dropRight(4)
//
//
//      val uploadFormInfoObject = new UploadFormInfo(emailID, apkFileName)
//
//      import scala.sys.process._
//      val commandLineOutput = "/Applications/Sireum/sireum amandroid taintAnalysis -m 2 -o /Users/rohitsharma/work/output/fcapps/ /Users/rohitsharma/work/source/fcapps/"+apkFileName !
//
//      println(commandLineOutput)
//
//
//      println(apkDir)
//
//      Redirect("/result/"+apkDir)
//    }.getOrElse {
//      Redirect("/upload").flashing(
//        "error" -> "File Not Uploaded"
//      )
//    }
//
//  }
//
//
//  def uploadFile = Action.async(parse.multipartFormData) { request =>
//    var apkFileName = ""
//    val futureString = scala.concurrent.Future {
//
//      request.body.file("file").map { apk =>
//        apkFileName = apk.filename
//        val emailID = request.body.dataParts.get("email").get
//
//        for (elem <- emailID) {emailAddress = elem }
//
//
//        println("email addree is: "+emailAddress)
//
//        apk.ref.moveTo(new File(s"/Users/rohitsharma/work/source/fcapps/$apkFileName"))
//
//
//      }.toString
//    }
//
//    val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Sending Email with link...", 1.second)
//    Future.firstCompletedOf(Seq(futureString, timeoutFuture)).map {
//      case s: String => Redirect("/executeamandroid/"+apkFileName)
//      case t: String => Ok("next page")
//    }
//  }
//
//
//  def executeAmandroid(apkFileName: String) = Action.async {
//    val apkDir = apkFileName.dropRight(4)
//    val apkDirPath = new File("/Users/rohitsharma/work/output/fcapps/" + apkDir)
//
//    println("directory path"+apkDirPath)
//
//    val bool = apkDirPath.exists()
//
//    println("boolean value is:"+bool)
//
//    if (bool) {
//      val futureInt = scala.concurrent.Future {
//        1
//      }
//      val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Some error occurred", 15.second)
//      Future.firstCompletedOf(Seq(futureInt, timeoutFuture)).map {
//        case i: Int => Redirect("/result/" + apkDir)
//        case t: String => Ok(t)
//      }
//    } else {
//      val futureInt = scala.concurrent.Future {
//        executeAmadroidHelper(apkFileName)
//
//      }
//      val timeoutFuture = play.api.libs.concurrent.Promise.timeout("http://localhost:9000/result/"+apkDir, 10.second)
//      Future.firstCompletedOf(Seq(futureInt, timeoutFuture)).map {
//        case i: Int => Redirect("/result/" + apkDir)
//        case t: String => Ok(views.html.emailform("http://localhost:9000/result/"+apkDir))
//      }
//    }
//  }
//
//  def executeAmadroidHelper(apkFileName: String): Int = {
//
//    println("Amandroid in execution...")
//
//    val value = 3
//
//    val commandRunExecution = "/Applications/Sireum/sireum amandroid taintAnalysis -m 2 -o /Users/rohitsharma/work/output/fcapps/ /Users/rohitsharma/work/source/fcapps/" + apkFileName !
//
//    println(commandRunExecution)
//    val apkDir = apkFileName.dropRight(4)
//    val message = "http://localhost:9000/result/" + apkDir
//    val absoluteDirectoryPath = "./public/" + apkDir
//    //zipResult(absoluteDirectoryPath)
//    sendEmailToUser(message, emailAddress, apkFileName)
//    value
//
//  }
//
//  //
//  def sendEmailToUser(message: String, to: String, apkFileName: String) = {
//    val email = Email(
//      apkFileName+" - Amandroid Result Ready",
//      "Sharma Rohit <rohit.sharma1048@gmail.com>",
//      Seq("Sharma Rohit <"+to+">"),
//       //adds attachment
////            attachments = Seq(
////              //adds zip file as an attachment
////              AttachmentFile(apkFileName.dropRight(4)+".zip", new File("./public/zip/"+apkFileName.dropRight(4)+".zip"))
////            ),
//      //sends text, HTML or both...
//      bodyText = Some("A text message"),
//      bodyHtml = Some(s"""<html><body><p><b>Please visit the link below to see amandroid results for $apkFileName</b><br><br> <code>$message</code></p></body></html>""")
//    )
//    mailerClient.send(email)
//
//  }
//
//  def zipResults = Action {
////    val absoluteDirectoryPath = "./public/FieldSensitivity4"
////
////    println("tar -cpzf ./public/zip/"+absoluteDirectoryPath.drop(9)+".zip "+absoluteDirectoryPath)
////
////    val commandZip = "tar -cpzf ./public/zip/"+absoluteDirectoryPath.drop(9)+".zip "+absoluteDirectoryPath !
////
////    //tar -cpzf zip/FileSenstivity4.zip FieldSensitivity4
////
////    println(commandZip)
//
//    sendEmailToUser("test", "rohit.sharma1048@gmail.com", "FieldSensitivity4.apk")
//
//    Ok("run completed")
//  }
//
//
//  def zipResult(absoluteDirectoryPath: String) = {
//
//    val commandZip = "tar -cpzf ./public/zip/"+absoluteDirectoryPath.drop(9)+".zip "+absoluteDirectoryPath !
//
//    //tar -cpzf zip/FileSenstivity4.zip FieldSensitivity4
//
//    println(commandZip)
//
//  }
//
//
//
//  def result(apkDir: String) = Action {
//
//
//    import java.io.File
//    val apkDirPath = new File("./public/"+apkDir)
//
//    if(apkDirPath.exists()){
//      println(apkDirPath.getAbsolutePath)
//
//      val aListInApkDirPath = apkDirPath.listFiles().toList
//
//      Ok(views.html.results(aListInApkDirPath))
//    } else {
//      Redirect("/upload")
//    }
//
//
//  }
//
//
//  def readFile(aFileName: String) = Action {
//    val contents = Source.fromFile(aFileName).mkString
//    Source.fromFile(aFileName).getLines().size
//    Source.fromFile(aFileName)
//    Ok(views.html.filecontent(aFileName))
//  }
//
//
//  def walkTree(file: File): Iterable[File] = {
//    val children = new Iterable[File] {
//      def iterator = if (file.isDirectory) file.listFiles.iterator else Iterator.empty
//    }
//    Seq(file) ++: children.flatMap(walkTree(_))
//  }
//
//
//  def actorTest = Action.async {
//
//    val futureInt = scala.concurrent.Future {
//      runAmandroid
//    }
//    val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Sending Email with link...", 5.second)
//    Future.firstCompletedOf(Seq(futureInt, timeoutFuture)).map {
//      case i: Int => Ok("Got result: " + i)
//      case t: String => Ok("")
//    }
//  }
//
//    def runAmandroid: Int = {
//      val value = 3
//
//      val myActor = actorSystem.actorOf(Props(new MyActor()))
//
//      (myActor ? PointToAnalysisObject) (5 minutes).mapTo[Future[Seq[Object]]]
//        .map(s => Await.result(s, Duration.Inf).foreach {
//          dr =>
//            println(dr)
//        })
//
//
//      value
//    }
//
//
//  def wsTest = Action.async {
//
//    val futureInt = scala.concurrent.Future {
//      wsTestHelper
//    }
//    val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Sending Email with link...", 5.second)
//    Future.firstCompletedOf(Seq(futureInt, timeoutFuture)).map {
//      case i: Int => Ok("Got result: " + i)
//      case t: String => Ok("results will be displayed later")
//    }
//  }
//
//  def wsTestHelper: Int = {
//    val value = 3
//
//     ws.url("http://localhost:8080/pta").withRequestTimeout(5 second).get()
//
//    value
//  }
//
//}
