/**
 *  Copyright 2011 Rapleaf
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.rapleaf.hank.hadoop.test;

import com.rapleaf.hank.hadoop.*;
import com.rapleaf.hank.storage.map.MapStorageEngine;
import com.rapleaf.hank.util.Bytes;
import org.apache.hadoop.mapred.TextInputFormat;

import java.io.IOException;
import java.nio.ByteBuffer;


public class TestMapStorageEngineCoordinator extends HadoopTestCase {

  private final String DOMAIN_A_NAME = "a";
  private final String INPUT_PATH_A = INPUT_DIR + "/" + DOMAIN_A_NAME;

  public TestMapStorageEngineCoordinator() throws IOException {
    super(TestMapStorageEngineCoordinator.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // Create inputs
    outputFile(fs, INPUT_PATH_A, "0 v0\n1 v1\n2 v2\n3 v3\n4 v4");
  }

  public void testOutput() throws IOException {
    new HadoopDomainBuilder(INPUT_PATH_A, TextInputFormat.class, TestHadoopDomainBuilder.TestMapper.class)
        .buildHankDomain(new DomainBuilderProperties(DOMAIN_A_NAME,
            MapStorageEngineCoordinator.getConfigurator(1), "/a", DomainBuilderEmptyOutputFormat.class), null);

    // Verify num partitions and num entries
    assertEquals(1, MapStorageEngine.getPartitions(DOMAIN_A_NAME).size());
    assertEquals(5, MapStorageEngine.getPartitions(DOMAIN_A_NAME).get(0).size());

    // Verify data
    assertEquals(0, Bytes.compareBytesUnsigned(ByteBuffer.wrap("v0".getBytes()), MapStorageEngine.getPartitions(DOMAIN_A_NAME).get(0).get(ByteBuffer.wrap("0".getBytes()))));
    assertEquals(0, Bytes.compareBytesUnsigned(ByteBuffer.wrap("v1".getBytes()), MapStorageEngine.getPartitions(DOMAIN_A_NAME).get(0).get(ByteBuffer.wrap("1".getBytes()))));
    assertEquals(0, Bytes.compareBytesUnsigned(ByteBuffer.wrap("v2".getBytes()), MapStorageEngine.getPartitions(DOMAIN_A_NAME).get(0).get(ByteBuffer.wrap("2".getBytes()))));
    assertEquals(0, Bytes.compareBytesUnsigned(ByteBuffer.wrap("v3".getBytes()), MapStorageEngine.getPartitions(DOMAIN_A_NAME).get(0).get(ByteBuffer.wrap("3".getBytes()))));
    assertEquals(0, Bytes.compareBytesUnsigned(ByteBuffer.wrap("v4".getBytes()), MapStorageEngine.getPartitions(DOMAIN_A_NAME).get(0).get(ByteBuffer.wrap("4".getBytes()))));
  }
}
