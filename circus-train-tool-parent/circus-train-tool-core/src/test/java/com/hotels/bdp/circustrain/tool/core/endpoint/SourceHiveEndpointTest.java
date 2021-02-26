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
package com.hotels.bdp.circustrain.tool.core.endpoint;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.base.Supplier;

import com.hotels.bdp.circustrain.api.conf.SourceTable;
import com.hotels.bdp.circustrain.api.conf.TableReplication;
import com.hotels.bdp.circustrain.core.TableAndStatistics;
import com.hotels.hcommon.hive.metastore.client.api.CloseableMetaStoreClient;

@RunWith(MockitoJUnitRunner.class)
public class SourceHiveEndpointTest {

  private @Mock HiveConf hiveConf;
  private @Mock Supplier<CloseableMetaStoreClient> metastoreSupplier;
  private @Mock TableReplication tableReplication;
  private @Mock CloseableMetaStoreClient metastoreClient;
  private @Mock Table table;
  private @Mock StorageDescriptor sd;
  private @Mock SourceTable sourceTable;

  @Test
  public void useCorrectReplicaTableName() throws Exception {
    SourceHiveEndpoint replicaDiffEndpoint = new SourceHiveEndpoint("name", hiveConf, metastoreSupplier);
    when(metastoreSupplier.get()).thenReturn(metastoreClient);
    when(metastoreClient.getTable("dbname", "tableName")).thenReturn(table);
    when(table.getSd()).thenReturn(sd);
    when(tableReplication.getSourceTable()).thenReturn(sourceTable);
    when(sourceTable.getDatabaseName()).thenReturn("dbname");
    when(sourceTable.getTableName()).thenReturn("tableName");
    TableAndStatistics tableAndStats = replicaDiffEndpoint.getTableAndStatistics(tableReplication);
    assertThat(tableAndStats.getTable(), is(table));

  }
}
