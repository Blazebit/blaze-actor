/*
 * Copyright 2018 - 2025 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.actor.spi;

/**
 * Information about a cluster node.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public interface ClusterNodeInfo {

    /**
     * Returns whether this node is the cluster coordinator.
     *
     * @return whether this node is the cluster coordinator
     */
    public boolean isCoordinator();

    /**
     * Returns a monotonically increasing cluster version number.
     * The version is increased after every cluster state change.
     *
     * @return the cluster version
     */
    public long getClusterVersion();

    /**
     * Returns the 0-based position within the cluster that can be used as partition key.
     *
     * @return the 0-based position within the cluster
     */
    public int getClusterPosition();

    /**
     * Returns the number of nodes in the cluster.
     *
     * @return the number of nodes in the cluster
     */
    public int getClusterSize();
}
