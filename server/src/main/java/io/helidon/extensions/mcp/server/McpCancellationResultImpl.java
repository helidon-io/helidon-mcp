/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

import java.util.Objects;
import java.util.Optional;

final class McpCancellationResultImpl implements McpCancellationResult {
    private final boolean requested;
    private final String reason;

    McpCancellationResultImpl(boolean requested) {
        this.requested = requested;
        this.reason = null;
    }

    McpCancellationResultImpl(boolean requested, String reason) {
        this.requested = requested;
        this.reason = Objects.requireNonNull(reason);
    }

    @Override
    public boolean isRequested() {
        return requested;
    }

    @Override
    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }
}
