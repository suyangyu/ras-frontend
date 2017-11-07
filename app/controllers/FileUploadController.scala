/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import java.nio.file.{Files, Paths}

import config.{FrontendAuthConnector, RasContext, RasContextImpl}
import connectors.UserDetailsConnector
import org.apache.commons.io.FileUtils
import play.Logger
import play.api.mvc.Action
import play.api.{Configuration, Environment, Play}
import services.UploadService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future
import scala.io.Source

trait FileUploadController extends RasController with PageFlowController {

  implicit val context: RasContext = RasContextImpl

  val fileUploadService: UploadService

  def get = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          Future.successful(Ok(views.html.file_upload()))
        case Left(resp) =>
          Logger.debug("[FileUploadController][get] user Not authorised")
          resp
      }
  }

  def post () = Action.async(parse.multipartFormData) {
    implicit request =>

      val dataAsByteArray = request.body.file("file").map { file =>
        import java.io.File
        val fileName = file.filename
        val contentType = file.contentType
        file.ref.moveTo(new File(s"tmp/files/$fileName"))
        Files.readAllBytes(Paths.get(s"tmp/files/$fileName"))
      }.getOrElse("".getBytes())

      fileUploadService.uploadFile(dataAsByteArray).map{ uploadSuccessful =>
        uploadSuccessful match {
          case true =>
            Logger.debug("[FileUploadController][post] File uploaded successfully")
            Redirect(routes.FileUploadController.uploadSuccessful())
          case _ =>
            Logger.debug("[FileUploadController][post] File upload failed")
            Redirect(routes.GlobalErrorController.get())
        }
      }

  }

  def post2 = Action.async { implicit request =>
    isAuthorised.flatMap{
      case Right(_) =>

        val file = request.body.asMultipartFormData.get.file("file").get.ref.file
        val fileAsArrayByte = FileUtils.readFileToByteArray(file)

        fileUploadService.uploadFile(fileAsArrayByte).map{ uploadSuccessful =>
          uploadSuccessful match {
            case true =>
              Logger.debug("[FileUploadController][post] File uploaded successfully")
              Redirect(routes.FileUploadController.uploadSuccessful())
            case _ =>
              Logger.debug("[FileUploadController][post] File upload failed")
              Redirect(routes.GlobalErrorController.get())
          }
        }
        Future.successful(Redirect(routes.GlobalErrorController.get()))
      case Left(res) => res
    }
  }




  def back = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userInfo) => Future.successful(previousPage("FileUploadController"))
        case Left(res) => res
      }
  }

  def uploadSuccessful = Action.async{
    implicit request =>
      isAuthorised.flatMap{
        case Right(_) => Future.successful(Ok(views.html.file_upload_successful()))
        case Left(res) => res
    }
  }

}

object FileUploadController extends FileUploadController {
  val authConnector: AuthConnector = FrontendAuthConnector
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
  val fileUploadService = UploadService
}
