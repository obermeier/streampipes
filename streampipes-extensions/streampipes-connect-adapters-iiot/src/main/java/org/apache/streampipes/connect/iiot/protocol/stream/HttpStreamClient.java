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

import org.apache.streampipes.commons.exceptions.connect.ParseException;

import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.io.InputStream;

/**
 * Executes the configured request and returns the response body.
 */
public class HttpStreamClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 1_000;
  private static final int SOCKET_TIMEOUT_MILLIS = 100_000;

  /**
   * Sends the request and returns the response body.
   *
   * @throws ParseException if the endpoint is unreachable or responds with a non-2xx status
   */
  public InputStream fetch(HttpRequestConfig config) {
    try {
      return toRequest(config)
          .execute()
          .returnContent()
          .asStream();
    } catch (IOException e) {
      throw new ParseException(
          "%s %s failed: %s".formatted(config.method(), config.url(), e.getMessage()), e);
    }
  }

  private Request toRequest(HttpRequestConfig config) {
    var request = switch (config.method()) {
      case GET -> Request.Get(config.url());
      case POST -> Request.Post(config.url()).bodyString(config.body(), config.contentType());
      case PUT -> Request.Put(config.url()).bodyString(config.body(), config.contentType());
    };

    request
        .connectTimeout(CONNECT_TIMEOUT_MILLIS)
        .socketTimeout(SOCKET_TIMEOUT_MILLIS);
    config.headers().forEach(header -> request.addHeader(header.name(), header.value()));

    return request;
  }
}
