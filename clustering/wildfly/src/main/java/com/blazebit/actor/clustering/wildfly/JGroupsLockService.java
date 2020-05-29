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

import com.blazebit.actor.spi.LockService;
import org.jgroups.Channel;

import java.util.concurrent.locks.Lock;

/**
 * A lock service implementation based on JGroups LockService.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public class JGroupsLockService implements LockService {

    private final org.jgroups.blocks.locking.LockService lockService;

    /**
     * Creates a lock service.
     *
     * @param channel The JGroups channel to use for the lock service
     */
    public JGroupsLockService(Channel channel) {
        this.lockService = new org.jgroups.blocks.locking.LockService(channel);
    }

    @Override
    public Lock getLock(String name) {
        return lockService.getLock(name);
    }

}
