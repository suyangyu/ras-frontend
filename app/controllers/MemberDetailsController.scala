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

import config.RasContextImpl
import connectors.{CustomerMatchingAPIConnector, ResidencyStatusAPIConnector}
import forms.MemberDetailsForm._
import helpers.helpers.I18nHelper
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object MemberDetailsController extends MemberDetailsController{
  override val customerMatchingAPIConnector = CustomerMatchingAPIConnector
  override val residencyStatusAPIConnector = ResidencyStatusAPIConnector
}

trait MemberDetailsController extends FrontendController with I18nHelper {

  val customerMatchingAPIConnector: CustomerMatchingAPIConnector
  val residencyStatusAPIConnector : ResidencyStatusAPIConnector

  implicit val context: config.RasContext = RasContextImpl

  def get = Action.async { implicit request =>
    Future.successful(Ok(views.html.member_details(form)))
  }

  def post = Action.async { implicit request =>

    form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.member_details(formWithErrors)))
      },
      memberDetails => {

        customerMatchingAPIConnector.findMemberDetails(memberDetails).flatMap { response =>

          val rasLink = response._links.filter( _.name == "ras").head.href

          val rs = residencyStatusAPIConnector.getResidencyStatus(rasLink).map { rs =>
            rs.currentYearResidencyStatus
          }

          Future.successful(Ok(views.html.match_found(rs)))
        }
      }
    )

  }
}
