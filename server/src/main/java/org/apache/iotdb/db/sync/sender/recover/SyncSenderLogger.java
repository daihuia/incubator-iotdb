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
package org.apache.iotdb.db.sync.sender.recover;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SyncSenderLogger implements ISyncSenderLogger {

  public static final String SYNC_DELETED_FILE_NAME_START = "sync deleted file names start";
  public static final String SYNC_TSFILE_START = "sync tsfile start";
  private BufferedWriter bw;

  public SyncSenderLogger(String filePath) throws IOException {
    this.bw = new BufferedWriter(new FileWriter(filePath));
  }

  public SyncSenderLogger(File file) throws IOException {
    this(file.getAbsolutePath());
  }

  @Override
  public void startSyncDeletedFilesName() throws IOException {
    bw.write(SYNC_DELETED_FILE_NAME_START);
    bw.newLine();
    bw.flush();
  }

  @Override
  public void finishSyncDeletedFileName(File file) throws IOException {
    bw.write(file.getAbsolutePath());
    bw.newLine();
    bw.flush();
  }

  @Override
  public void startSyncTsFiles() throws IOException {
    bw.write(SYNC_TSFILE_START);
    bw.newLine();
    bw.flush();
  }

  @Override
  public void finishSyncTsfile(File file) throws IOException {
    bw.write(file.getAbsolutePath());
    bw.newLine();
    bw.flush();
  }

  @Override
  public void close() throws IOException {
    bw.close();
  }
}