/**
 * Copyright (C) 2018-2020 Expedia, Inc.
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
package com.expediagroup.apiary.extensions.readonlyauth.listener;

import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.MetaStorePreEventListener;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.InvalidOperationException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.events.PreEventContext;
import org.apache.hadoop.hive.metastore.events.PreReadDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.PreReadTableEvent;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MetaStorePreEventListener to authorize access using a static database list.
 */
public class ApiaryReadOnlyAuthPreEventListener extends MetaStorePreEventListener {

  private static final Logger log = LoggerFactory.getLogger(ApiaryReadOnlyAuthPreEventListener.class);

  private List<String> databaseWhitelist = null;

  public ApiaryReadOnlyAuthPreEventListener(Configuration config) throws HiveException {
    super(config);

    String databaseNames = System.getenv("HIVE_DB_WHITELIST");
    if (databaseNames == null || databaseNames.equals("")) {
      throw new IllegalArgumentException("HIVE_DB_WHITELIST System envrionment variable not defined or empty");
    }
    databaseWhitelist = Arrays.asList(databaseNames.split(","));
    databaseWhitelist.replaceAll(s -> s.trim());

    log.debug("ApiaryReadOnlyAuthPreEventListener created");
  }

  @Override
  public void onEvent(PreEventContext context) throws MetaException, NoSuchObjectException, InvalidOperationException {

    switch (context.getEventType()) {

    case READ_TABLE:
      Table table = ((PreReadTableEvent) context).getTable();
      applyWhitelist(table.getDbName());
      break;

    case READ_DATABASE:
      Database db = ((PreReadDatabaseEvent) context).getDatabase();
      applyWhitelist(db.getName());
      break;

    default:
      throw new InvalidOperationException(context.getEventType() + " is disabled from read-only metastores.");
    }

  }

  private void applyWhitelist(String databaseName) throws InvalidOperationException {
    if (!databaseWhitelist.contains(databaseName)) {
      throw new InvalidOperationException(databaseName + " database is not in allowed list:" + databaseWhitelist);
    }
  }

}
