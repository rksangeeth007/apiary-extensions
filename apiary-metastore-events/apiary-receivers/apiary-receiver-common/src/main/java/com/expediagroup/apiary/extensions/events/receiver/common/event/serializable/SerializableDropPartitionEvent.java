/**
 * Copyright (C) 2019 Expedia, Inc.
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
package com.expediagroup.apiary.extensions.events.receiver.common.event.serializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.events.DropPartitionEvent;

public class SerializableDropPartitionEvent extends SerializableListenerEvent {
  private static final long serialVersionUID = 1L;

  private Table table;
  private List<Partition> partitions;
  private boolean deleteData;

  SerializableDropPartitionEvent() {}

  public SerializableDropPartitionEvent(DropPartitionEvent event) {
    super(event);
    deleteData = event.getDeleteData();
    table = event.getTable();
    partitions = new ArrayList<Partition>();
    Iterator<Partition> iterator = event.getPartitionIterator();
    while (iterator.hasNext()) {
      partitions.add(iterator.next());
    }
  }

  @Override
  public String getDatabaseName() {
    return table.getDbName();
  }

  @Override
  public String getTableName() {
    return table.getTableName();
  }

  public Table getTable() {
    return table;
  }

  public List<Partition> getPartitions() {
    return partitions;
  }

  public boolean getDeleteData() {
    return deleteData;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SerializableDropPartitionEvent)) {
      return false;
    }
    SerializableDropPartitionEvent other = (SerializableDropPartitionEvent) obj;
    return super.equals(other)
        && Objects.equals(deleteData, other.deleteData)
        && Objects.equals(table, other.table)
        && Objects.equals(partitions, other.partitions);
  }

}
