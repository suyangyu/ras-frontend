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

package connectors

import config.ApplicationConfig
import helpers.RandomNino
import models.{CustomerMatchingResponse, Link, MemberDetails, RasDate}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpPost, HttpResponse}

import scala.concurrent.Future

class CustomerMatchingAPIConnectorSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ServicesConfig {

  val mockHttp = mock[HttpPost]

  object TestCustomerMatchingAPIConnector extends CustomerMatchingAPIConnector {
    override val applicationConfig = mock[ApplicationConfig]
  }

  "Customer Matching API connector" should {

    // In order to TDD this connector we need a test to drive the code around the http call not itself
    "handle redirect" in {

      val memberDetails = MemberDetails(RandomNino.generate, "Ramin", "Esfandiari", RasDate("1","1","1999"))
      val customerDetails = memberDetails.asCustomerDetails

      val er = CustomerMatchingResponse(List(
        Link("self","/customer/matched/633e0ee7-315b-49e6-baed-d79c3dffe467"),
        Link("relief-at-source","/relief-at-source/customer/633e0ee7-315b-49e6-baed-d79c3dffe467/get-residency-status")))


      when(mockHttp.POST[HttpResponse,HttpResponse](any(),any())(any(),any(), any())).
        thenReturn(Future.successful(HttpResponse(200, Some(Json.toJson(er)))))

      val result = await(TestCustomerMatchingAPIConnector.findMemberDetails(customerDetails))

      result shouldBe er

    }


  }

}
