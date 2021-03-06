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
package com.hotels.bdp.circustrain.core.replica.hive;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.hotels.bdp.circustrain.api.data.DataManipulator;
import com.hotels.hcommon.hive.metastore.client.api.CloseableMetaStoreClient;

@RunWith(MockitoJUnitRunner.class)
public class DropTableServiceTest {

  private static final String TABLE_NAME = "table";
  private static final String DB_NAME = "db";
  private static final Path REPLICA_LOCATION = new Path("replica_table_location");
  private static final String PARTITION_LOCATION = "partition_location";

  private @Mock CloseableMetaStoreClient client;
  private @Captor ArgumentCaptor<Table> tableCaptor;

  private @Mock DataManipulator dataManipulator;
  private @Mock StorageDescriptor storageDescriptor;

  private DropTableService service;
  private Table table = new Table();

  @Before
  public void setUp() throws TException {
    service = new DropTableService();
    table.setTableName(TABLE_NAME);
    table.setDbName(DB_NAME);
    when(client.getTable(DB_NAME, TABLE_NAME)).thenReturn(table);

    storageDescriptor.setLocation(REPLICA_LOCATION.toString());
    table.setSd(storageDescriptor);
  }

  @Test
  public void removeParamsAndDropNullParams() throws Exception {
    service.dropTable(client, DB_NAME, TABLE_NAME);

    verify(client).dropTable(DB_NAME, TABLE_NAME, false, true);
    verify(client).getTable(DB_NAME, TABLE_NAME);
    verifyNoMoreInteractions(client);
  }

  @Test
  public void removeParamsAndDropEmptyParams() throws Exception {
    table.setParameters(Collections.emptyMap());

    service.dropTable(client, DB_NAME, TABLE_NAME);

    verify(client).getTable(DB_NAME, TABLE_NAME);
    verify(client).dropTable(DB_NAME, TABLE_NAME, false, true);
    verifyNoMoreInteractions(client);
  }

  @Test
  public void removeParamsAndDrop() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("key1", "value");
    params.put("key2", "value");
    params.put("EXTERNAL", "true");
    table.setParameters(params);

    service.dropTable(client, DB_NAME, TABLE_NAME);

    verify(client).getTable(DB_NAME, TABLE_NAME);
    verify(client).alter_table(eq(DB_NAME), eq(TABLE_NAME), tableCaptor.capture());
    verify(client).dropTable(DB_NAME, TABLE_NAME, false, true);
    verifyNoMoreInteractions(client);
    List<Table> capturedTables = tableCaptor.getAllValues();
    assertThat(capturedTables.size(), is(1));
    Map<String, String> parameters = capturedTables.get(0).getParameters();
    assertThat(parameters.size(), is(1));
    assertThat(parameters.get("EXTERNAL"), is("TRUE"));
  }

  @Test
  public void removeParamsAndDropCaseInsensitiveExternalTable() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("key1", "value");
    params.put("key2", "value");
    params.put("external", "TRUE");
    table.setParameters(params);

    service.dropTable(client, DB_NAME, TABLE_NAME);

    verify(client).getTable(DB_NAME, TABLE_NAME);
    verify(client).alter_table(eq(DB_NAME), eq(TABLE_NAME), tableCaptor.capture());
    verify(client).dropTable(DB_NAME, TABLE_NAME, false, true);
    verifyNoMoreInteractions(client);
    List<Table> capturedTables = tableCaptor.getAllValues();
    assertThat(capturedTables.size(), is(1));
    Map<String, String> parameters = capturedTables.get(0).getParameters();
    assertThat(parameters.size(), is(1));
    assertThat(parameters.get("EXTERNAL"), is("TRUE"));
  }

  @Test
  public void removeParamsAndDropTableDoesNotExist() throws Exception {
    doThrow(new NoSuchObjectException()).when(client).getTable(DB_NAME, TABLE_NAME);

    service.dropTable(client, DB_NAME, TABLE_NAME);

    verify(client).getTable(DB_NAME, TABLE_NAME);
    verifyNoMoreInteractions(client);
  }

  @Test
  public void dropTableAndDataSuccess() throws Exception {
    table.setParameters(Collections.emptyMap());

    service.dropTableAndData(client, DB_NAME, TABLE_NAME, dataManipulator);

    verify(client).getTable(DB_NAME, TABLE_NAME);
    verify(client).dropTable(DB_NAME, TABLE_NAME, false, true);
    verifyNoMoreInteractions(client);
  }

  @Test
  public void dropTableAndDataTableDoesNotExist() throws Exception {
    doThrow(new NoSuchObjectException()).when(client).getTable(DB_NAME, TABLE_NAME);

    service.dropTableAndData(client, DB_NAME, TABLE_NAME, dataManipulator);

    verify(client).getTable(DB_NAME, TABLE_NAME);
    verifyNoMoreInteractions(client);
  }

  @Test
  public void dropPartitionedTableSuccess() throws Exception {
    List<String> partitionNames = Arrays.asList("name", "surname");
    List<Partition> partitions = createPartitions(partitionNames.size());

    when(client.listPartitionNames(DB_NAME, TABLE_NAME, (short) -1)).thenReturn(partitionNames);
    when(client.getPartitionsByNames(DB_NAME, TABLE_NAME, partitionNames)).thenReturn(partitions);
    table.setPartitionKeys(createFieldSchemaList(partitionNames));

    service.dropTableAndData(client, DB_NAME, TABLE_NAME, dataManipulator);

    verify(client).getTable(DB_NAME, TABLE_NAME);
    verify(client).dropTable(DB_NAME, TABLE_NAME, false, true);
    verify(dataManipulator).delete(PARTITION_LOCATION + "1");
    verify(dataManipulator).delete(PARTITION_LOCATION + "2");
  }

  @Test
  public void dropPartitionedTableMultipleBatches() throws Exception {
    int count = 1001;
    List<Partition> partitionsBatch1 = createPartitions(count);
    List<Partition> partitionsBatch2 = Arrays.asList(partitionsBatch1.remove(count - 1));

    List<String> batch1 = new ArrayList<String>();
    List<String> batch2 = Arrays.asList("other");
    for (int i = 1; i < count; i++) {
      batch1.add("name");
    }

    List<String> partitionNames = new ArrayList<>();
    partitionNames.addAll(batch1);
    partitionNames.addAll(batch2);

    table.setPartitionKeys(createFieldSchemaList(partitionNames));

    when(client.listPartitionNames(DB_NAME, TABLE_NAME, (short) -1)).thenReturn(partitionNames);
    when(client.getPartitionsByNames(DB_NAME, TABLE_NAME, batch1)).thenReturn(partitionsBatch1);
    when(client.getPartitionsByNames(DB_NAME, TABLE_NAME, batch2)).thenReturn(partitionsBatch2);

    service.dropTableAndData(client, DB_NAME, TABLE_NAME, dataManipulator);

    verify(client).getTable(DB_NAME, TABLE_NAME);
    verify(client).dropTable(DB_NAME, TABLE_NAME, false, true);

    for (int i = 1; i < count + 1; i++) {
      verify(dataManipulator).delete(PARTITION_LOCATION + i);
    }
  }

  @Test
  public void failToDeleteData() throws Exception {
    List<String> partitionNames = Arrays.asList("name", "surname");
    List<Partition> partitions = createPartitions(partitionNames.size());

    when(client.listPartitionNames(DB_NAME, TABLE_NAME, (short) -1)).thenReturn(partitionNames);
    when(client.getPartitionsByNames(DB_NAME, TABLE_NAME, partitionNames)).thenReturn(partitions);
    when(dataManipulator.delete(PARTITION_LOCATION + "1")).thenThrow(new IOException());
    table.setPartitionKeys(createFieldSchemaList(partitionNames));

    try {
      service.dropTableAndData(client, DB_NAME, TABLE_NAME, dataManipulator);
      fail("Expected exception should be caught and thrown");
    } catch (Exception e) {
      verify(client).getTable(DB_NAME, TABLE_NAME);
      verify(client, never()).dropTable(DB_NAME, TABLE_NAME, false, true);
      verify(dataManipulator).delete(PARTITION_LOCATION + "1");
      verifyNoMoreInteractions(dataManipulator);
    }
  }

  private List<Partition> createPartitions(int count) {
    List<Partition> partitions = new ArrayList<>();
    for (int i = 1; i < count + 1; i++) {
      Partition partition = new Partition();
      partition.setSd(new StorageDescriptor());
      partition.getSd().setLocation(PARTITION_LOCATION + i);
      partitions.add(partition);
    }
    return partitions;
  }

  private List<FieldSchema> createFieldSchemaList(List<String> names) {
    List<FieldSchema> fieldSchemas = new ArrayList<>();
    for (String name : names) {
      fieldSchemas.add(new FieldSchema(name, "String", ""));
    }
    return fieldSchemas;
  }

}
