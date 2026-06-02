package io.sequenceforge.gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class SequenceGenerationSimulation extends Simulation {

  val baseUrl    = System.getProperty("base.url",    "http://localhost:8080")
  val apiKey     = System.getProperty("api.key",     "REPLACE_ME")
  val templateId = System.getProperty("template.id", "REPLACE_ME")

  val httpProtocol = http
    .baseUrl(baseUrl)
    .header("Content-Type", "application/json")
    .header("X-Api-Key", apiKey)
    .acceptHeader("application/json")

  val generateSequence = scenario("Generate Sequence")
    .exec(
      http("POST /api/v1/sequences/generate")
        .post("/api/v1/sequences/generate")
        .body(StringBody(
          s"""{"templateId":"$templateId","params":{"ss":"MH","cc":"IN"}}"""
        ))
        .check(status.is(200))
        .check(jsonPath("$.success").is("true"))
        .check(jsonPath("$.data.sequence").exists)
    )

  // Ramp to 8000 RPS over 2 minutes, hold for 5 minutes
  setUp(
    generateSequence.inject(
      rampUsersPerSec(1).to(8000).during(2.minutes),
      constantUsersPerSec(8000).during(5.minutes)
    )
  )
    .protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(95).lt(50),   // p95 < 50ms
      global.responseTime.percentile(99).lt(200),  // p99 < 200ms
      global.successfulRequests.percent.gt(99.9)   // 99.9% success rate
    )
}
