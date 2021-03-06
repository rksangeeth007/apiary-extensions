/**
 * Copyright (C) 2018-2019 Expedia, Inc.
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
package com.expediagroup.apiary.extensions.events.metastore.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class EventTypeTest {

  @Test
  public void eventClassesAreUnique() {
    Map<Class<? extends ApiaryListenerEvent>, EventType> cache = new HashMap<>();
    for (EventType et : EventType.values()) {
      assertThat(cache).doesNotContainKey(et.eventClass());
      cache.put(et.eventClass(), et);
    }
    assertThat(cache.values()).hasSize(EventType.values().length);
  }
}
