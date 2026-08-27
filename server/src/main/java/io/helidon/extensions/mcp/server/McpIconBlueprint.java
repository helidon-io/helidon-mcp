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

import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.media.type.MediaType;

/**
 * Icon metadata.
 */
@Prototype.Blueprint
@Prototype.Configured
@Prototype.CustomMethods(McpIconSupport.class)
interface McpIconBlueprint {
    /**
     * Icon source. This must be an {@code http} or {@code https} URL, or a
     * {@code data} URI with a Base64-encoded payload.
     *
     * @return icon source
     */
    @Option.Configured
    @Option.Decorator(McpDecorators.IconSourceDecorator.class)
    String source();

    /**
     * Optional MIME type override.
     *
     * @return MIME type
     */
    @Option.Configured
    Optional<MediaType> mediaType();

    /**
     * Icon sizes, each formatted as {@code WxH} or {@code any}.
     *
     * @return icon sizes
     */
    @Option.Singular
    @Option.Configured
    List<String> sizes();

    /**
     * Optional display theme.
     *
     * @return icon theme
     */
    @Option.Configured
    Optional<McpIconTheme> theme();
}
