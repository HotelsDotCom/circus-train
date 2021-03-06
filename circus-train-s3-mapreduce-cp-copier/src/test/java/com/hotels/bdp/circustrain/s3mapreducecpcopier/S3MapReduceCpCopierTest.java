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
package com.hotels.bdp.circustrain.s3mapreducecpcopier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.CANNED_ACL;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.COPY_STRATEGY;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.CREDENTIAL_PROVIDER;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.IGNORE_FAILURES;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.LOG_PATH;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.MAX_MAPS;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.MULTIPART_UPLOAD_CHUNK_SIZE;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.MULTIPART_UPLOAD_THRESHOLD;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.NUMBER_OF_WORKERS_PER_MAP;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.REGION;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.S3_SERVER_SIDE_ENCRYPTION;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.STORAGE_CLASS;
import static com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpOptionsParser.TASK_BANDWIDTH;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.security.alias.CredentialProviderFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.StorageClass;
import com.codahale.metrics.MetricRegistry;

import com.hotels.bdp.circustrain.api.copier.CopierOptions;
import com.hotels.bdp.circustrain.api.metrics.Metrics;
import com.hotels.bdp.circustrain.s3mapreducecp.S3MapReduceCpOptions;
import com.hotels.bdp.circustrain.s3mapreducecp.SimpleCopyListing;
import com.hotels.bdp.circustrain.s3mapreducecpcopier.S3MapReduceCpCopier.S3MapReduceCpExecutor;

@RunWith(MockitoJUnitRunner.class)
public class S3MapReduceCpCopierTest {

  private @Mock S3MapReduceCpExecutor executor;
  private @Mock Job job;
  private final Map<String, Object> copierOptions = new HashMap<>();
  private @Mock MetricRegistry metricRegistry;

  private @Captor ArgumentCaptor<Configuration> confCaptor;
  private @Captor ArgumentCaptor<S3MapReduceCpOptions> optionsCaptor;

  private final Configuration conf = new Configuration();
  private final Path sourceDataBaseLocation = new Path("hdfs://source/");
  private final Path replicaDataLocation = new Path("s3://target/");
  private final URI credentialsProvider = URI.create("jceks://hdfs/path/to/credentials.jceks");

  @Before
  public void setupLibJarPath() throws Exception {
    conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, credentialsProvider.toString());
    when(job.waitForCompletion(anyBoolean())).thenReturn(true);
    when(executor.exec(any(Configuration.class), any(S3MapReduceCpOptions.class))).thenReturn(job);
  }

  @Test
  public void tableArgsAndConfiguration() throws Exception {
    S3MapReduceCpCopier copier = new S3MapReduceCpCopier(conf, sourceDataBaseLocation, Collections.<Path>emptyList(),
        replicaDataLocation, copierOptions, executor, metricRegistry);
    Metrics metrics = copier.copy();
    assertThat(metrics, not(nullValue()));

    verify(executor).exec(confCaptor.capture(), optionsCaptor.capture());

    S3MapReduceCpOptions options = optionsCaptor.getValue();
    assertThat(options.getSources(), is(Arrays.asList(sourceDataBaseLocation)));
    assertThat(options.getTarget(), is(replicaDataLocation.toUri()));
    assertThat(options.getCredentialsProvider(), is(credentialsProvider));
  }

  @Test
  public void tableArgsAndConfigurationOneFileCopy() throws Exception {
    Path sourceDataBaseFile = new Path("hdfs://source/data.txt");
    Path replicaDataFile = new Path("s3://target/data.txt");
    copierOptions.put(CopierOptions.COPY_DESTINATION_IS_FILE, "true");
    S3MapReduceCpCopier copier = new S3MapReduceCpCopier(conf, sourceDataBaseFile, Collections.<Path>emptyList(),
        replicaDataFile, copierOptions, executor, metricRegistry);
    Metrics metrics = copier.copy();
    assertThat(metrics, not(nullValue()));

    verify(executor).exec(confCaptor.capture(), optionsCaptor.capture());

    S3MapReduceCpOptions options = optionsCaptor.getValue();
    assertThat(options.getSources(), is(Arrays.asList(sourceDataBaseFile)));
    assertThat(options.getTarget(), is(replicaDataFile.toUri()));
    assertThat(options.getCredentialsProvider(), is(credentialsProvider));
  }

  @Test
  public void overwriteAllCopierOptions() throws Exception {
    copierOptions.put(CREDENTIAL_PROVIDER, "jceks://hdfs/foo/bar.jceks");
    copierOptions.put(MULTIPART_UPLOAD_CHUNK_SIZE, "1234");
    copierOptions.put(S3_SERVER_SIDE_ENCRYPTION, "true");
    copierOptions.put(STORAGE_CLASS, "reduced_redundancy");
    copierOptions.put(TASK_BANDWIDTH, "567");
    copierOptions.put(NUMBER_OF_WORKERS_PER_MAP, "89");
    copierOptions.put(MULTIPART_UPLOAD_THRESHOLD, "123456");
    copierOptions.put(MAX_MAPS, "78");
    copierOptions.put(COPY_STRATEGY, "the-strategy");
    copierOptions.put(LOG_PATH, "hdfs://path/to/logs/");
    copierOptions.put(REGION, "us-east-1");
    copierOptions.put(IGNORE_FAILURES, "true");
    copierOptions.put(CANNED_ACL, CannedAccessControlList.BucketOwnerFullControl.toString());

    S3MapReduceCpCopier copier = new S3MapReduceCpCopier(conf, sourceDataBaseLocation, Collections.<Path>emptyList(),
        replicaDataLocation, copierOptions, executor, metricRegistry);
    Metrics metrics = copier.copy();
    assertThat(metrics, not(nullValue()));

    verify(executor).exec(confCaptor.capture(), optionsCaptor.capture());

    S3MapReduceCpOptions options = optionsCaptor.getValue();
    assertThat(options.getSources(), is(Arrays.asList(sourceDataBaseLocation)));
    assertThat(options.getTarget(), is(replicaDataLocation.toUri()));
    assertThat(options.getCredentialsProvider(), is(URI.create("jceks://hdfs/foo/bar.jceks")));
    assertThat(options.getMultipartUploadPartSize(), is(1234L));
    assertThat(options.isS3ServerSideEncryption(), is(true));
    assertThat(options.getStorageClass(), is(StorageClass.ReducedRedundancy.toString()));
    assertThat(options.getMaxBandwidth(), is(567L));
    assertThat(options.getNumberOfUploadWorkers(), is(89));
    assertThat(options.getMultipartUploadThreshold(), is(123456L));
    assertThat(options.getMaxMaps(), is(78));
    assertThat(options.getCopyStrategy(), is("the-strategy"));
    assertThat(options.getLogPath(), is(new Path("hdfs://path/to/logs/")));
    assertThat(options.getRegion(), is(Regions.US_EAST_1.getName()));
    assertThat(options.isIgnoreFailures(), is(true));
    assertThat(options.getCannedAcl(), is(CannedAccessControlList.BucketOwnerFullControl.toString()));
  }

  @Test
  public void partitionsArgsAndConfiguration() throws Exception {
    List<Path> partitionLocations = Arrays
        .asList(new Path(sourceDataBaseLocation, "p1"), new Path(sourceDataBaseLocation, "p2"));
    S3MapReduceCpCopier copier = new S3MapReduceCpCopier(conf, sourceDataBaseLocation, partitionLocations,
        replicaDataLocation, copierOptions, executor, metricRegistry);

    copier.copy();

    verify(executor).exec(confCaptor.capture(), optionsCaptor.capture());

    Configuration config = confCaptor.getValue();
    assertThat(config.get(SimpleCopyListing.CONF_LABEL_ROOT_PATH), is(sourceDataBaseLocation.toUri().toString()));

    S3MapReduceCpOptions options = optionsCaptor.getValue();
    assertThat(options.getSources(), is(partitionLocations));
    assertThat(options.getTarget(), is(replicaDataLocation.toUri()));
    assertThat(options.getCredentialsProvider(), is(credentialsProvider));
  }

}
