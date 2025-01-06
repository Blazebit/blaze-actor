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
package com.blazebit.actor.clustering.wildfly;

import com.blazebit.actor.spi.ClusterNodeInfo;
import com.blazebit.actor.spi.ClusterStateListener;
import com.blazebit.actor.spi.ClusterStateManager;
import com.blazebit.actor.spi.LockService;
import com.blazebit.actor.spi.StateReturningEvent;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Christian Beikov
 * @since 1.0.0
 */
public class WildflyClusterStateManager implements ClusterStateManager {

    private static final Logger LOG = Logger.getLogger(WildflyClusterStateManager.class.getName());
    private static final Node[] EMPTY = new Node[0];
    private static final Comparator<Node> NODE_COMPARATOR = new Comparator<Node>() {

        @Override
        public int compare(Node o1, Node o2) {
            byte[] b1 = o1.getSocketAddress().getAddress().getAddress();
            byte[] b2 = o2.getSocketAddress().getAddress().getAddress();
            for (int i = 0; i < b1.length; i++) {
                int cmp = b1[i] - b2[i];
                if (cmp != 0) {
                    return cmp;
                }
            }

            return Integer.compare(o1.getSocketAddress().getPort(), o2.getSocketAddress().getPort());
        }

    };

//    @Resource(lookup = "java:jboss/clustering/group/ee")
    private final Group channelGroup;
//    @Resource(lookup = "java:jboss/clustering/dispatcher/ee")
    private final CommandDispatcherFactory factory;
//    @Resource(lookup = "java:jboss/jgroups/channel/ee")
    private final LockService lockService;

    private final List<ClusterStateListener> clusterStateListeners = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, List<java.util.function.Consumer<Serializable>>> listeners = new ConcurrentHashMap<>();
    private final Node[] localNode;
    private final AtomicReference<ClusterNodeInfo> currentNodeInfo = new AtomicReference<>(new DefaultClusterNodeInfo(true, "127.0.0.1", 0L, 0, 1));
    private volatile CommandDispatcher<WildflyClusterStateManager> fireEventDispatcher;

    /**
     * Creates a new cluster state manager for the given clustering channel group and clustering dispatcher factory.
     *
     * @param channelGroup The channel group
     * @param factory The dispatcher factory
     * @param lockService The cluster lock service
     */
    public WildflyClusterStateManager(Group channelGroup, CommandDispatcherFactory factory, LockService lockService) {
        this.channelGroup = channelGroup;
        this.factory = factory;
        this.lockService = lockService;
        this.localNode = new Node[]{channelGroup.getLocalNode()};
    }

    /**
     * Starts the cluster state manager.
     */
    public void start() {
        fireEventDispatcher = this.factory.createCommandDispatcher("fireEventDispatcher", this);
        final Node localNode = channelGroup.getLocalNode();
        updateCurrentPosition(localNode, channelGroup.getNodes());
        channelGroup.addListener(new Group.Listener() {
            @Override
            public void membershipChanged(List<Node> previousMembers, List<Node> members, boolean merged) {
                updateCurrentPosition(localNode, members);
            }
        });
    }

    /**
     * Stops the cluster state manager.
     */
    public void close() {
        fireEventDispatcher.close();
    }

    @Override
    public ClusterNodeInfo getCurrentNodeInfo() {
        return currentNodeInfo.get();
    }

    @Override
    public void registerListener(ClusterStateListener listener) {
        listener.onClusterStateChanged(currentNodeInfo.get());
        clusterStateListeners.add(listener);
    }

    @Override
    public <T extends Serializable> void registerListener(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add((java.util.function.Consumer<Serializable>) listener);
    }

    private <T> T fireEventLocally(StateReturningEvent<T> event) {
        fireEventLocally((Serializable) event);
        return event.getResult();
    }

    private void fireEventLocally(Serializable event) {
        java.util.function.Consumer<Class<?>> consumer = eventClass -> {
            List<java.util.function.Consumer<Serializable>> consumers = listeners.get(eventClass);
            if (consumers != null) {
                consumers.forEach(c -> c.accept(event));
            }
        };
        Class<?> clazz = event.getClass();
        Set<Class<?>> visitedClasses = new HashSet<>();
        do {
            consumer.accept(clazz);
            visitInterfaces(consumer, clazz, visitedClasses);
            clazz = clazz.getSuperclass();
        } while (clazz != null);
    }

    private void visitInterfaces(java.util.function.Consumer<Class<?>> consumer, Class<?> clazz, Set<Class<?>> visitedClasses) {
        Class<?>[] interfaces = clazz.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            Class<?> interfaceClass = interfaces[i];
            if (visitedClasses.add(interfaceClass)) {
                consumer.accept(interfaceClass);
                visitInterfaces(consumer, interfaceClass, visitedClasses);
            }
        }
    }

    @Override
    public void fireEvent(Serializable event, boolean await) {
        fireEvent(event, EMPTY, await);
    }

    @Override
    public void fireEventExcludeSelf(Serializable event, boolean await) {
        fireEvent(event, localNode, await);
    }

    @Override
    public <T> Map<ClusterNodeInfo, Future<T>> fireEvent(StateReturningEvent<T> event) {
        return fireEvent(event, EMPTY);
    }

    @Override
    public <T> Map<ClusterNodeInfo, Future<T>> fireEventExcludeSelf(StateReturningEvent<T> event) {
        return fireEvent(event, localNode);
    }

    private void fireEvent(Serializable event, Node[] excludedNodes, boolean await) {
        try {
            Map<Node, CommandResponse<Object>> results = fireEventDispatcher.executeOnCluster(new FireEventCommand(event), excludedNodes);
            List<Throwable> exceptions = null;
            for (final Map.Entry<Node, CommandResponse<Object>> result : results.entrySet()) {
                if (await) {
                    Object response;
                    if (result.getValue() != null) {
                        try {
                            response = result.getValue().get();
                            LOG.fine(() -> "Command result: Node [" + result.getKey().getName() + "@" + result.getKey().getSocketAddress().toString() + "]: " + response);
                        } catch (ExecutionException ex) {
                            if (exceptions == null) {
                                exceptions = new ArrayList<>();
                            }

                            exceptions.add(ex);
                            LOG.fine(() -> "Command exception: Node [" + result.getKey().getName() + "@" + result.getKey().getSocketAddress().toString() + "]: " + ex);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Could not broadcast!", ex);
        }
    }

    private <T> Map<ClusterNodeInfo, Future<T>> fireEvent(StateReturningEvent<T> event, Node[] excludedNodes) {
        try {
            Map<ClusterNodeInfo, Future<T>> map = new HashMap<>();
            Map<Node, CommandResponse<T>> results = fireEventDispatcher.executeOnCluster(new FireStateReturningEventCommand<>(event), excludedNodes);
            Node coordinatorNode = channelGroup.getCoordinatorNode();
            List<Node> nodes = getSortedNodeList();
            ClusterNodeInfo clusterNodeInfo = currentNodeInfo.get();
            for (final Map.Entry<Node, CommandResponse<T>> result : results.entrySet()) {
                Node node = result.getKey();
                int index = nodes.indexOf(node);
                ClusterNodeInfo nodeInfo = new DefaultClusterNodeInfo(coordinatorNode.equals(node), node.getSocketAddress().getAddress().getHostAddress(), clusterNodeInfo.getClusterVersion(), index, nodes.size());
                map.put(nodeInfo, new CommandResponseFuture<>(result.getValue()));
            }
            return map;
        } catch (Exception ex) {
            throw new RuntimeException("Could not broadcast!", ex);
        }
    }

    @Override
    public LockService getLockService() {
        return lockService;
    }

    @Override
    public boolean isStandalone() {
        return false;
    }

    /**
     * @author Christian Beikov
     * @since 1.0.0
     */
    private static class FireEventCommand implements Command<Object, WildflyClusterStateManager> {
        private static final long serialVersionUID = 1L;
        private final Serializable event;

        public FireEventCommand(Serializable event) {
            this.event = event;
        }

        @Override
        public Object execute(WildflyClusterStateManager context) throws Exception {
            context.fireEventLocally(event);
            return "";
        }

    }

    /**
     * @author Christian Beikov
     * @since 1.0.0
     */
    private static class FireStateReturningEventCommand<T> implements Command<T, WildflyClusterStateManager> {
        private static final long serialVersionUID = 1L;
        private final StateReturningEvent<T> event;

        public FireStateReturningEventCommand(StateReturningEvent<T> event) {
            this.event = event;
        }

        @Override
        public T execute(WildflyClusterStateManager context) throws Exception {
            return context.fireEventLocally(event);
        }

    }

    /**
     * @author Christian Beikov
     * @since 1.0.0
     */
    private static class CommandResponseFuture<T> implements Future<T> {
        private static final long serialVersionUID = 1L;
        private final CommandResponse<T> response;

        public CommandResponseFuture(CommandResponse<T> response) {
            this.response = response;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return response.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return response.get();
        }

    }

    private List<Node> getSortedNodeList() {
        return getSortedNodeList(channelGroup.getLocalNode(), channelGroup.getNodes());
    }

    private List<Node> getSortedNodeList(Node localNode, List<Node> nodes) {
        List<Node> nodeList = new ArrayList<>(nodes.size() + 1);

        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (!localNode.equals(n)) {
                nodeList.add(n);
            }
        }

        nodeList.add(localNode);
        nodeList.sort(NODE_COMPARATOR);
        return nodeList;
    }

    private void updateCurrentPosition(Node localNode, List<Node> nodes) {
        List<Node> nodeList = getSortedNodeList(localNode, nodes);
        int newPosition = nodeList.indexOf(localNode);

        boolean isCoordinator = channelGroup.isCoordinator();

        ClusterNodeInfo old = currentNodeInfo.get();
        ClusterNodeInfo nodeInfo = new DefaultClusterNodeInfo(isCoordinator, localNode.getSocketAddress().getAddress().getHostAddress(), old.getClusterVersion() + 1L, newPosition, nodeList.size());
        currentNodeInfo.compareAndSet(old, nodeInfo);
        LOG.info("Updated cluster position to: " + newPosition + " of " + members(nodeList));
        LOG.info("ChannelGroup members: " + members(channelGroup.getNodes()));
        clusterStateListeners.forEach(l -> l.onClusterStateChanged(nodeInfo));
    }

    private static String members(List<Node> nodeList) {
        return nodeList.stream().map(node -> node.getSocketAddress().getAddress().getHostAddress()).collect(Collectors.joining(", ", "(", ")"));
    }

}
