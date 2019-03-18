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

import java.io._
import java.net.InetSocketAddress

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

/**
 * This app is a simplified example of an Enrich Remote Adapter.
 *
 * It can also be used to validate the External integration tests of the scala-common-enrich RemoteAdapterSpec test class:
 * if you set the externalEnrichmentUrl in that test class to Some("http://127.0.0.1:8995/sampleRemoteAdapter"),
 * then the External tests in that class should pass whenever this app is running.
 */
object Server extends App {
  val tcpPort  = 8995
  val pathName = "sampleRemoteAdapter"

  val httpServer = localHttpServer(tcpPort, pathName)
  httpServer.start()

  val sampleAdapter = new SampleAdapter()

  private def localHttpServer(tcpPort: Int, basePath: String): HttpServer = {
    val httpServer = HttpServer.create(new InetSocketAddress(tcpPort), 0)

    httpServer.createContext(
      s"/$basePath",
      new HttpHandler {
        def handle(exchange: HttpExchange): Unit = {

          val response = sampleAdapter.handle(getBodyAsString(exchange.getRequestBody))

          exchange.sendResponseHeaders(200, 0)
          exchange.getResponseBody.write(response.getBytes)
          exchange.getResponseBody.close()
        }
      }
    )
    httpServer
  }

  private def getBodyAsString(body: InputStream): String = {
    val s = new java.util.Scanner(body).useDelimiter("\\A")
    if (s.hasNext) s.next() else ""
  }

}
