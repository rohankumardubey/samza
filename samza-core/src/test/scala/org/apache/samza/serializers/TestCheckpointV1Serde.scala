/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.samza.serializers

import java.util

import org.apache.samza.Partition
import org.apache.samza.checkpoint.{CheckpointV1}
import org.apache.samza.container.TaskName
import org.apache.samza.system.SystemStreamPartition
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._

class TestCheckpointV1Serde {
  @Test
  def testExactlyOneOffset {
    val serde = new CheckpointV1Serde
    var offsets = Map[SystemStreamPartition, String]()
    val systemStreamPartition = new SystemStreamPartition("test-system", "test-stream", new Partition(777))
    offsets += systemStreamPartition -> "1"
    val deserializedOffsets = serde.fromBytes(serde.toBytes(new CheckpointV1(offsets.asJava)))
    assertEquals("1", deserializedOffsets.getOffsets.get(systemStreamPartition))
    assertEquals(1, deserializedOffsets.getOffsets.size)
  }

  @Test
  def testNullCheckpointSerde: Unit = {
    val checkpointBytes = null.asInstanceOf[Array[Byte]]
    val checkpointSerde = new CheckpointV1Serde
    val checkpoint = checkpointSerde.fromBytes(checkpointBytes)
    assertNull(checkpoint)
  }
}
