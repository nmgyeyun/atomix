/*
 * Copyright 2018-present Open Networking Foundation
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
package io.atomix.protocols.raft.storage.log;

import io.atomix.protocols.raft.storage.log.index.RaftLogIndex;
import io.atomix.utils.serializer.Namespace;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Mappable log segment writer.
 */
public class MappableLogSegmentWriter<E> implements RaftLogWriter<E> {
  private final File file;
  private final FileChannel channel;
  private final RaftLogSegmentDescriptor descriptor;
  private final int maxEntrySize;
  private final RaftLogSegmentCache cache;
  private final RaftLogIndex index;
  private final Namespace namespace;
  private RaftLogWriter<E> writer;

  public MappableLogSegmentWriter(
      File file,
      FileChannel channel,
      RaftLogSegmentDescriptor descriptor,
      int maxEntrySize,
      RaftLogSegmentCache cache,
      RaftLogIndex index,
      Namespace namespace) {
    this.file = file;
    this.channel = channel;
    this.descriptor = descriptor;
    this.maxEntrySize = maxEntrySize;
    this.cache = cache;
    this.index = index;
    this.namespace = namespace;
    this.writer = new RaftLogSegmentWriter<>(file, channel, descriptor, maxEntrySize, cache, index, namespace);
  }

  MappedByteBuffer map() {
    try {
      MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, descriptor.maxSegmentSize());
      this.writer = new MappedLogSegmentWriter<>(file, buffer, descriptor, maxEntrySize, index, namespace);
      return buffer;
    } catch (IOException e) {
      throw new RaftIOException(e);
    }
  }

  void unmap() {
    this.writer = new RaftLogSegmentWriter<>(file, channel, descriptor, maxEntrySize, cache, index, namespace);
  }

  public long firstIndex() {
    return descriptor.index();
  }

  @Override
  public long getLastIndex() {
    return writer.getLastIndex();
  }

  @Override
  public Indexed<E> getLastEntry() {
    return writer.getLastEntry();
  }

  @Override
  public long getNextIndex() {
    return writer.getNextIndex();
  }

  @Override
  public <T extends E> Indexed<T> append(T entry) {
    return writer.append(entry);
  }

  @Override
  public void append(Indexed<E> entry) {
    writer.append(entry);
  }

  @Override
  public void truncate(long index) {
    writer.truncate(index);
  }

  @Override
  public void flush() {
    writer.flush();
  }

  @Override
  public void close() {
    writer.close();
  }

  public void delete() {
    if (writer instanceof MappedLogSegmentWriter) {
      ((MappedLogSegmentWriter<E>) writer).delete();
    } else if (writer instanceof RaftLogSegmentWriter) {
      ((RaftLogSegmentWriter<E>) writer).delete();
    }
  }
}
