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

import java.util.Arrays;
import java.util.Locale;

/**
 * HTTP methods supported by the HTTP stream adapter.
 *
 * <p>The internal ids of the UI properties are derived from the method name
 * (e.g. {@code http-method-post}, {@code post-request-body}), so adding a method only
 * requires a new constant here and the matching labels in {@code strings.en}.
 */
public enum HttpMethod {

  GET(false),
  POST(true),
  PUT(true);

  private final boolean hasBody;

  HttpMethod(boolean hasBody) {
    this.hasBody = hasBody;
  }

  public boolean hasBody() {
    return hasBody;
  }

  public String alternativeId() {
    return "http-method-" + key();
  }

  public String bodyOptionsId() {
    return key() + "-options";
  }

  public String contentTypeId() {
    return key() + "-content-type";
  }

  public String requestBodyId() {
    return key() + "-request-body";
  }

  public static HttpMethod fromAlternativeId(String alternativeId) {
    return Arrays.stream(values())
        .filter(method -> method.alternativeId().equals(alternativeId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown HTTP method: " + alternativeId));
  }

  private String key() {
    return name().toLowerCase(Locale.ROOT);
  }
}
