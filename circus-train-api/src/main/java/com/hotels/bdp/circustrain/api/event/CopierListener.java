/**
 * Copyright (C) 2016-2021 Expedia, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.bdp.circustrain.api.event;

import com.hotels.bdp.circustrain.api.metrics.Metrics;

public interface CopierListener {

  /**
   * Is guaranteed to be called before a {@link com.hotels.bdp.circustrain.api.copier.Copier#copy()} is called.
   *
   * @param copierImplementation
   */
  void copierStart(String copierImplementation);

  /**
   * Is guaranteed to be called when {@link com.hotels.bdp.circustrain.api.copier.Copier#copy()} is finished (even when {@link com.hotels.bdp.circustrain.api.copier.Copier#copy()} throws
   * exceptions).
   *
   * @param metrics
   */
  void copierEnd(Metrics metrics);

}
