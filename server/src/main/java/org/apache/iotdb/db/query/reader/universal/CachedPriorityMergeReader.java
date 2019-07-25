/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.query.reader.universal;

import java.io.IOException;
import org.apache.iotdb.db.utils.TimeValuePair;

/**
 * CachedPriorityMergeReader use a cache to reduce unnecessary heap updates and increase locality.
 */
public class CachedPriorityMergeReader extends PriorityMergeReader {

  private static final int CACHE_SIZE = 100;

  private TimeValuePair[] timeValuePairCache = new TimeValuePair[CACHE_SIZE];
  private int cacheLimit = 0;
  private int cacheIdx = -1;

  @Override
  public boolean hasNext() {
    return cacheIdx + 1 < cacheLimit || !heap.isEmpty();
  }

  private void fetch() throws IOException {
    cacheLimit = 0;
    cacheIdx = -1;
    while (!heap.isEmpty() && cacheLimit < CACHE_SIZE) {
      Element top = heap.poll();
      if (cacheLimit == 0 || top.currTime() != timeValuePairCache[cacheLimit - 1].getTimestamp()) {
        timeValuePairCache[cacheLimit++] = top.timeValuePair;
        if (top.hasNext()) {
          top.next();
          heap.add(top);
        } else {
          top.close();
        }
      } else if (top.currTime() == timeValuePairCache[cacheLimit - 1].getTimestamp()) {
        if (top.hasNext()) {
          top.next();
          heap.add(top);
        } else {
          top.close();
        }
      }
    }
  }

  @Override
  public TimeValuePair next() throws IOException {
    TimeValuePair ret;
    if (cacheIdx + 1 < cacheLimit) {
      ret = timeValuePairCache[++cacheIdx];
    } else {
      fetch();
      ret = timeValuePairCache[++cacheIdx];
    }
    return ret;
  }

  @Override
  public TimeValuePair current() {
    if (0 <= cacheIdx && cacheIdx < cacheLimit) {
      return timeValuePairCache[cacheIdx];
    } else {
      return heap.peek().timeValuePair;
    }
  }
}