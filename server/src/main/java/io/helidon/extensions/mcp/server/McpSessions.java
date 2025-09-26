/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.extensions.mcp.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import io.helidon.common.LruCache;

/**
 * An LRU cache for {@link McpSession}s that also implements {@link Iterable}.
 */
final class McpSessions implements LruCache<String, McpSession>, Iterable<McpSession> {

    private final List<String> sessionsList;
    private final LruCache<String, McpSession> sessionsMap;

    McpSessions(int cacheSize) {
        sessionsMap = LruCache.create(cacheSize);
        sessionsList = new CopyOnWriteArrayList<>();
    }

    public Optional<McpSession> get(String sessionId) {
        return sessionsMap.get(sessionId);
    }

    public Optional<McpSession> put(String sessionId, McpSession session) {
        sessionsList.add(sessionId);
        return sessionsMap.put(sessionId, session);
    }

    public Optional<McpSession> remove(String sessionId) {
        sessionsList.remove(sessionId);
        return sessionsMap.remove(sessionId);
    }

    @Override
    public int size() {
        return sessionsMap.size();
    }

    @Override
    public int capacity() {
        return sessionsMap.capacity();
    }

    @Override
    public void clear() {
        sessionsMap.clear();
        sessionsList.clear();
    }

    @Override
    public Optional<McpSession> computeValue(String key, Supplier<Optional<McpSession>> valueSupplier) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Iterator<McpSession> iterator() {
        ArrayList<McpSession> activeSessions = new ArrayList<>();
        sessionsList.forEach(sessionId -> {
            Optional<McpSession> session = sessionsMap.get(sessionId);
            session.ifPresent(activeSessions::add);
        });
        return activeSessions.iterator();
    }
}
