/*
 * Copyright (c) 2019-2019 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.example

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JArray
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write

class SampleAdapter() {

  val sampleTracker = "remote-adapter-example-v1.0.0"
  val samplePlatform = "srv"
  val sampleSchemaVendor = "com.snowplowanalytics.snowplow"
  val sampleSchemaName = "remote_adapter_example"
  val sampleSchemaFormat = "jsonschema"
  val sampleSchemaVersion = "1-0-0"

  implicit val formats = DefaultFormats

  sealed case class Payload(
                             queryString: Map[String, String],
                             headers: List[String],
                             body: Option[String],
                             contentType: Option[String]
                           )

  sealed case class Response(
                              events: List[Map[String, String]],
                              error: String
                            )

  def handle(body: String) =
    try {
      parse(body).extract[Payload] match {
        case payload: Payload =>
          parse (payload.body.get) \ "group"  match {
            case JArray(list) =>
              val output = list.map { item =>
                val json =
                  ("schema" -> "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0") ~
                    ("data" -> (("schema" -> s"iglu:$sampleSchemaVendor/$sampleSchemaName/$sampleSchemaFormat/$sampleSchemaVersion") ~
                      ("data" -> item)))

                Map(("tv" -> sampleTracker),
                  ("e" -> "ue"),
                  ("p" -> payload.queryString.getOrElse("p", samplePlatform)),
                  ("ue_pr" -> write(json))
                ) ++ payload.queryString
              }
              write(Response(output,null))
          }
          case _ =>  write(Response(null, s"expecting a a List field called `group` in the data`"))

        case anythingElse =>
          write(Response(null, s"expecting a payload json but got a ${anythingElse.getClass}"))
      }
    } catch {
      case e: Exception => write(Response(null, s"aack, sampleAdapter exception $e"))
    }
}
