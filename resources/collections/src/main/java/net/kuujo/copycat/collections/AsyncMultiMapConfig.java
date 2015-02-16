/*
 * Copyright 2014 the original author or authors.
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
package net.kuujo.copycat.collections;

import com.typesafe.config.ConfigValueFactory;
import net.kuujo.copycat.collections.internal.map.MultiMapState;
import net.kuujo.copycat.raft.Consistency;
import net.kuujo.copycat.resource.ResourceConfig;
import net.kuujo.copycat.state.StateMachineConfig;
import net.kuujo.copycat.util.internal.Assert;

import java.util.Map;

/**
 * Asynchronous multi-map configuration.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class AsyncMultiMapConfig extends ResourceConfig<AsyncMultiMapConfig> {
  public static final String ASYNC_MULTIMAP_CONSISTENCY = "consistency";

  private static final String DEFAULT_CONFIGURATION = "multi-map-defaults";
  private static final String CONFIGURATION = "multi-map";

  public AsyncMultiMapConfig() {
    super(CONFIGURATION, DEFAULT_CONFIGURATION);
  }

  public AsyncMultiMapConfig(Map<String, Object> config) {
    super(config, CONFIGURATION, DEFAULT_CONFIGURATION);
  }

  public AsyncMultiMapConfig(String resource) {
    super(resource, CONFIGURATION, DEFAULT_CONFIGURATION);
    setDefaultName(resource);
  }

  protected AsyncMultiMapConfig(AsyncMultiMapConfig config) {
    super(config);
  }

  @Override
  public AsyncMultiMapConfig copy() {
    return new AsyncMultiMapConfig(this);
  }

  /**
   * Sets the multimap read consistency.
   *
   * @param consistency The multimap read consistency.
   * @throws java.lang.NullPointerException If the consistency is {@code null}
   */
  public void setConsistency(String consistency) {
    this.config = config.withValue(ASYNC_MULTIMAP_CONSISTENCY, ConfigValueFactory.fromAnyRef(Consistency.parse(Assert.notNull(consistency, "consistency")).toString()));
  }

  /**
   * Sets the multimap read consistency.
   *
   * @param consistency The multimap read consistency.
   * @throws java.lang.NullPointerException If the consistency is {@code null}
   */
  public void setConsistency(Consistency consistency) {
    this.config = config.withValue(ASYNC_MULTIMAP_CONSISTENCY, ConfigValueFactory.fromAnyRef(Assert.notNull(consistency, "consistency").toString()));
  }

  /**
   * Returns the multimap read consistency.
   *
   * @return The multimap read consistency.
   */
  public Consistency getConsistency() {
    return Consistency.parse(config.getString(ASYNC_MULTIMAP_CONSISTENCY));
  }

  /**
   * Sets the multimap read consistency, returning the configuration for method chaining.
   *
   * @param consistency The multimap read consistency.
   * @return The multimap configuration.
   * @throws java.lang.NullPointerException If the consistency is {@code null}
   */
  public AsyncMultiMapConfig withConsistency(String consistency) {
    setConsistency(consistency);
    return this;
  }

  /**
   * Sets the multimap read consistency, returning the configuration for method chaining.
   *
   * @param consistency The multimap read consistency.
   * @return The multimap configuration.
   * @throws java.lang.NullPointerException If the consistency is {@code null}
   */
  public AsyncMultiMapConfig withConsistency(Consistency consistency) {
    setConsistency(consistency);
    return this;
  }

  @Override
  public ResourceConfig<?> resolve() {
    return new StateMachineConfig(toMap())
      .withStateType(MultiMapState.class)
      .withInitialState(MultiMapState.class)
      .resolve();
  }

}
