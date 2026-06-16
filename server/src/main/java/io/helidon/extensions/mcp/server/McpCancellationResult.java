/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
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

import java.util.Optional;

/**
 * The cancellation result includes an indication of whether
 * a cancellation request was made, along with the optional reason for request cancellation.
 */
public sealed interface McpCancellationResult permits McpCancellationResultImpl {
    /**
     * Check if cancellation was requested.
     *
     * @return {@code true} if cancellation was requested, {@code false} otherwise
     */
    boolean isRequested();

    /**
     * Cancellation reason provided by the client, when available.
     *
     * @return optional cancellation reason
     */
    Optional<String> reason();
}
