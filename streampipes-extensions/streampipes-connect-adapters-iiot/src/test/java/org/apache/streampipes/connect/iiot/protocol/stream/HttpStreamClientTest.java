/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.streampipes.connect.iiot.protocol.stream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Sends real requests to an in-process HTTP server and checks what arrives.
 */
class HttpStreamClientTest {

  private static final String RESPONSE = "{\"temperature\": 21.5}";
  private static final String BODY = "{\"query\": \"all\"}";
  private static final String LOOPBACK = "127.0.0.1";

  private final HttpStreamClient client = new HttpStreamClient();
  private final AtomicReference<ReceivedRequest> received = new AtomicReference<>();

  private HttpServer server;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);
    server.createContext("/", exchange -> {
      var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      received.set(new ReceivedRequest(exchange.getRequestMethod(), exchange.getRequestHeaders(), body));

      var response = RESPONSE.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, response.length);
      try (var out = exchange.getResponseBody()) {
        out.write(response);
      }
    });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void fetch_shouldSendGetWithoutBody() throws IOException {
    var response = client.fetch(HttpRequestConfig.withoutBody(HttpMethod.GET, url(), List.of()));

    assertEquals(RESPONSE, new String(response.readAllBytes(), StandardCharsets.UTF_8));
    assertEquals("GET", received.get().method());
    assertEquals("", received.get().body());
    assertNull(received.get().headers().getFirst("Content-Type"));
  }

  @Test
  void fetch_shouldSendPostWithBodyAndContentType() {
    client.fetch(HttpRequestConfig.withBody(HttpMethod.POST, url(), List.of(), "application/json", BODY));

    assertEquals("POST", received.get().method());
    assertEquals(BODY, received.get().body());
    assertEquals("application/json; charset=UTF-8", received.get().headers().getFirst("Content-Type"));
  }

  @Test
  void fetch_shouldSendPutWithBodyAndContentType() {
    client.fetch(HttpRequestConfig.withBody(HttpMethod.PUT, url(), List.of(), "text/plain", "hello"));

    assertEquals("PUT", received.get().method());
    assertEquals("hello", received.get().body());
    assertEquals("text/plain; charset=UTF-8", received.get().headers().getFirst("Content-Type"));
  }

  @Test
  void fetch_shouldSendCustomHeaders() {
    var headers = List.of(new HttpRequestConfig.Header("Authorization", "Bearer token"));

    client.fetch(HttpRequestConfig.withoutBody(HttpMethod.GET, url(), headers));

    assertEquals("Bearer token", received.get().headers().getFirst("Authorization"));
  }

  private String url() {
    return "http://" + LOOPBACK + ":" + server.getAddress().getPort() + "/data";
  }

  private record ReceivedRequest(String method, Headers headers, String body) {
  }
}
