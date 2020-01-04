/*
 * Copyright 2018 - 2020 Blazebit.
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

package com.blazebit.actor.clustering.wildfly;

import com.blazebit.actor.spi.ClusterNodeInfo;

/**
 * A default cluster node information POJO.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public class DefaultClusterNodeInfo implements ClusterNodeInfo {

    private final boolean coordinator;
    private final String ipAddress;
    private final long clusterVersion;
    private final int clusterPosition;
    private final int clusterSize;

    /**
     * Creates a new cluster node info based on the given arguments.
     *
     * @param coordinator Whether the node is the coordinator
     * @param ipAddress The ip address of the node
     * @param clusterVersion The cluster version
     * @param clusterPosition The 0-based position of the node in the cluster
     * @param clusterSize The cluster size
     */
    public DefaultClusterNodeInfo(boolean coordinator, String ipAddress, long clusterVersion, int clusterPosition, int clusterSize) {
        this.coordinator = coordinator;
        this.ipAddress = ipAddress;
        this.clusterVersion = clusterVersion;
        this.clusterPosition = clusterPosition;
        this.clusterSize = clusterSize;
    }

    @Override
    public boolean isCoordinator() {
        return coordinator;
    }

    @Override
    public long getClusterVersion() {
        return clusterVersion;
    }

    /**
     * Returns the ip address of the node.
     *
     * @return the ip address of the node
     */
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public int getClusterPosition() {
        return clusterPosition;
    }

    @Override
    public int getClusterSize() {
        return clusterSize;
    }
}
