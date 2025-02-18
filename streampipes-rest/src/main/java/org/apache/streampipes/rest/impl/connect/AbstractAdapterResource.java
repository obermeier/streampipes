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
package org.apache.streampipes.rest.impl.connect;

import org.apache.streampipes.model.client.user.DefaultPrivilege;
import org.apache.streampipes.rest.core.base.impl.AbstractAuthGuardedRestResource;

import java.util.function.Supplier;

public class AbstractAdapterResource<T> extends AbstractAuthGuardedRestResource {

  protected T managementService;

  public AbstractAdapterResource(Supplier<T> managementServiceSupplier) {
    this.managementService = managementServiceSupplier.get();
  }

  /**
   * required by Spring expression
   */
  public boolean hasReadAuthority() {
    return isAdminOrHasAnyAuthority(DefaultPrivilege.Constants.PRIVILEGE_READ_ADAPTER_VALUE);
  }

  /**
   * required by Spring expression
   */
  public boolean hasWriteAuthority() {
    return isAdminOrHasAnyAuthority(DefaultPrivilege.Constants.PRIVILEGE_WRITE_ADAPTER_VALUE);
  }
}
