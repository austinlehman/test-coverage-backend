package com.gatling.tests.AdminTests

import com.gatling.tests.Modules.AdminModules.*
import com.gatling.tests.Modules.HeaderModules.*
import com.gatling.tests.Modules.LoginModule.{adminHomePage, adminLoginScenario}
import io.gatling.core.Predef.*
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.*
import io.gatling.jdbc.Predef.*

import scala.concurrent.duration.*

class OrderListAdminTest extends Simulation {

  //Scenario that tests adding order
  val orderAdd: ScenarioBuilder = scenario("Admins Adding Order")
    .exec(adminLoginScenario) //Log into system as admin
    .exec { session =>
      val newSession = session.setAll("operation" -> "Add", //Set up general session info
        "endpoint" -> "adminbasicservice/adminorder",
        "file_path" -> "OrderListAdmin/add_order_form.json")
      newSession
    }
    //Go to order page and complete add
    .exec(adminHomePage, completeAction, adminHomePage)
    .pause(1)

  //Scenario that tests deleting contact
  val orderDelete: ScenarioBuilder = scenario("Admins Deleting Order")
    .exec(adminLoginScenario)
    .exec { session => //Set up session information
      val newSession = session.setAll("delete_id" -> "301f39ba-f31d-4795-bac2-cbc8909a7e97/G1237",
        "endpoint" -> "adminorderservice/adminorder",
        "type" -> "Order")
      newSession
    }
    //Go to order page and delete order
    .exec(adminHomePage, delete, adminHomePage)
    .pause(1)

  //TODO: Add feeder for update files

  setUp(
    orderAdd.inject(rampUsers(20).during(15)),
    orderDelete.inject(rampUsers(20).during(15))
  ).protocols(httpProtocolTrainTicket)
}