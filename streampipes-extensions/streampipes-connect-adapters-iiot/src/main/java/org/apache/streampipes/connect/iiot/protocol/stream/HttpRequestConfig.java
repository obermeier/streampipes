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

import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Immutable description of the request the HTTP stream adapter sends on every poll.
 *
 * @param method      HTTP method
 * @param url         endpoint URL
 * @param headers     custom request headers
 * @param contentType content type of the body, {@code null} for methods without body
 * @param body        request body, {@code null} for methods without body
 */
public record HttpRequestConfig(HttpMethod method,
                                String url,
                                List<Header> headers,
                                ContentType contentType,
                                String body) {

  public static final String DEFAULT_CONTENT_TYPE = "application/json";

  public HttpRequestConfig {
    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(url, "url");
    headers = List.copyOf(headers);
    if (method.hasBody()) {
      Objects.requireNonNull(contentType, "contentType");
      body = Objects.requireNonNullElse(body, "");
    }
  }

  public static HttpRequestConfig withoutBody(HttpMethod method, String url, List<Header> headers) {
    return new HttpRequestConfig(method, url, headers, null, null);
  }

  /**
   * Creates a request with a body.
   *
   * @throws IllegalArgumentException if the content type cannot be parsed
   */
  public static HttpRequestConfig withBody(HttpMethod method,
                                           String url,
                                           List<Header> headers,
                                           String contentType,
                                           String body) {
    return new HttpRequestConfig(method, url, headers, parseContentType(contentType), body);
  }

  /**
   * Parses a content type such as {@code application/json; charset=ISO-8859-1}.
   * Blank values fall back to {@value #DEFAULT_CONTENT_TYPE}, a missing charset to UTF-8.
   */
  static ContentType parseContentType(String value) {
    var contentType = value == null || value.isBlank() ? DEFAULT_CONTENT_TYPE : value.trim();
    try {
      var parsed = ContentType.parse(contentType);
      return parsed.getCharset() != null
          ? parsed
          : parsed.withParameters(new BasicNameValuePair("charset", StandardCharsets.UTF_8.name()));
    } catch (org.apache.http.ParseException | IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid content type: '" + contentType + "'", e);
    }
  }

  public record Header(String name, String value) {

    public Header {
      Objects.requireNonNull(name, "name");
      value = Objects.requireNonNullElse(value, "");
    }
  }
}
