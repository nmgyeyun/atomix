/*
 * Copyright 2015 the original author or authors.
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
package net.kuujo.copycat.raft;

import net.kuujo.copycat.cluster.Member;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Raft cluster member.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class RaftMember {
  private static final int FAILURE_LIMIT = 3;
  private Member.Type type;
  private Member.Status status;
  private long changed;
  private String uri;
  private long version = 1;
  private Long index;
  private Set<String> failures = new HashSet<>();

  public RaftMember() {
  }

  public RaftMember(String uri, Member.Type type, Member.Status status) {
    this(uri, type, status, 1);
  }

  public RaftMember(String uri, Member.Type type, Member.Status status, long version) {
    this.uri = uri;
    this.type = type;
    this.status = status;
    this.version = version;
  }

  /**
   * Returns the member type.
   *
   * @return The member type.
   */
  public Member.Type type() {
    return type;
  }

  /**
   * Returns the member status.
   *
   * @return The member status.
   */
  public Member.Status status() {
    return status;
  }

  /**
   * Returns the last time the member state changed.
   *
   * @return The last time the member state changed.
   */
  public long changed() {
    return changed;
  }

  /**
   * Returns the member URI.
   *
   * @return The member URI.
   */
  public String uri() {
    return uri;
  }

  /**
   * Returns the member version.
   *
   * @return The member version.
   */
  public long version() {
    return version;
  }

  /**
   * Sets the member version.
   *
   * @param version The member version.
   * @return The member info.
   */
  RaftMember version(long version) {
    this.version = version;
    return this;
  }

  /**
   * Returns the member's last log index.
   *
   * @return The member's last log index.
   */
  public Long index() {
    return index;
  }

  /**
   * Sets the member index.
   *
   * @param index The member's last log index.
   * @return The member info.
   */
  RaftMember index(Long index) {
    this.index = index;
    return this;
  }

  /**
   * Marks a successful gossip with the member.
   *
   * @return The member info.
   */
  public RaftMember succeed() {
    if (type == Member.Type.PASSIVE && status != Member.Status.ALIVE) {
      failures.clear();
      status = Member.Status.ALIVE;
      changed = System.currentTimeMillis();
    }
    return this;
  }

  /**
   * Marks a failure in the member.
   *
   * @param uri The URI recording the failure.
   * @return The member info.
   */
  public RaftMember fail(String uri) {
    // If the member is a passive member, add the failure to the failures set and change the state. If the current
    // state is ACTIVE then change the state to SUSPICIOUS. If the current state is SUSPICIOUS and the number of
    // failures from *unique* nodes is equal to or greater than the failure limit then change the state to DEAD.
    if (type == Member.Type.PASSIVE) {
      failures.add(uri);
      if (status == Member.Status.ALIVE) {
        status = Member.Status.SUSPICIOUS;
        changed = System.currentTimeMillis();
      } else if (status == Member.Status.SUSPICIOUS) {
        if (failures.size() >= FAILURE_LIMIT) {
          status = Member.Status.DEAD;
          changed = System.currentTimeMillis();
        }
      }
    }
    return this;
  }

  /**
   * Updates the member info.
   *
   * @param info The member info to update.
   */
  public void update(RaftMember info) {
    // If the given version is greater than the current version then update the member state.
    if (info.version > this.version) {
      this.type = info.type;
      this.version = info.version;
      this.index = info.index;

      // Any time the version is incremented, clear failures for the previous version.
      this.failures.clear();

      // Only passive member types can experience state changes. Active members are always alive.
      if (this.type == Member.Type.PASSIVE) {
        // If the state changed then update the state and set the last changed time. This can be used to clean
        // up state related to old members after some period of time.
        if (this.status != info.status) {
          changed = System.currentTimeMillis();
        }
        this.status = info.status;
      }
    } else if (info.version == this.version) {
      this.type = info.type;
      if (info.status == Member.Status.SUSPICIOUS) {
        // If the given version is the same as the current version then update failures. If the member has experienced
        // FAILURE_LIMIT failures then transition the member's state to DEAD.
        this.failures.addAll(info.failures);
        if (this.failures.size() >= FAILURE_LIMIT) {
          this.status = Member.Status.DEAD;
          changed = System.currentTimeMillis();
        }
      } else if (info.status == Member.Status.DEAD) {
        this.status = Member.Status.DEAD;
        changed = System.currentTimeMillis();
      }
    }
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof RaftMember) {
      RaftMember member = (RaftMember) object;
      return member.uri.equals(uri)
        && member.type == type
        && member.status == status
        && member.version == version
        && member.index == index;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hashCode = 17;
    hashCode = 37 * hashCode + uri.hashCode();
    hashCode = 37 * hashCode + type.hashCode();
    hashCode = 37 * hashCode + status.hashCode();
    if (index != null) hashCode = 37 * hashCode + (int)(index ^ (index >>> 32));
    hashCode = 37 * hashCode + (int)(version ^ (version >>> 32));
    return hashCode;
  }

  @Override
  public String toString() {
    return String.format("RaftMember[uri=%s, type=%s, state=%s, version=%d, index=%s]", uri, type, status, version, index);
  }

}
