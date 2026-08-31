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

import java.net.URI;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import io.helidon.builder.api.Prototype;

import static io.helidon.extensions.mcp.server.McpPagination.DEFAULT_PAGE_SIZE;

/**
 * Placeholder for the MCP configuration decorators.
 */
final class McpDecorators {
    private McpDecorators() {
    }

    static boolean isPositiveAndLessThanOne(Double value) {
        return 0 <= value && value <= 1.0;
    }

    /**
     * Enforce positive page size.
     * <p>
     * See {@link io.helidon.extensions.mcp.server.McpPagination}.
     */
    static class PageSizeDecorator implements Prototype.OptionDecorator<McpServerConfig.BuilderBase<?, ?>, Integer> {
        @Override
        public void decorate(McpServerConfig.BuilderBase<?, ?> builder, Integer pageSize) {
            if (pageSize < DEFAULT_PAGE_SIZE) {
                throw new IllegalArgumentException("Page size must be greater than zero");
            }
        }
    }

    /**
     * Enforce intelligence priority value between 0 and 1.
     * <p>
     * See {@link io.helidon.extensions.mcp.server.McpSamplingRequest}.
     */
    static class IntelligencePriorityDecorator implements Prototype.OptionDecorator<McpSamplingRequest.BuilderBase<?, ?>, Optional<Double>> {
        @Override
        public void decorate(McpSamplingRequest.BuilderBase<?, ?> builder, Optional<Double> value) {
            value.filter(McpDecorators::isPositiveAndLessThanOne)
                    .orElseThrow(() -> new IllegalArgumentException("Intelligence priority must be in range [0, 1]"));
        }
    }

    /**
     * Enforce speed priority value between 0 and 1.
     * <p>
     * See {@link io.helidon.extensions.mcp.server.McpSamplingRequest}.
     */
    static class SpeedPriorityDecorator implements Prototype.OptionDecorator<McpSamplingRequest.BuilderBase<?, ?>, Optional<Double>> {
        @Override
        public void decorate(McpSamplingRequest.BuilderBase<?, ?> builder, Optional<Double> value) {
            value.filter(McpDecorators::isPositiveAndLessThanOne)
                    .orElseThrow(() -> new IllegalArgumentException("Speed priority must be in range [0, 1]"));
        }
    }

    /**
     * Enforce cost priority value between 0 and 1.
     * <p>
     * See {@link io.helidon.extensions.mcp.server.McpSamplingRequest}.
     */
    static class CostPriorityDecorator implements Prototype.OptionDecorator<McpSamplingRequest.BuilderBase<?, ?>, Optional<Double>> {
        @Override
        public void decorate(McpSamplingRequest.BuilderBase<?, ?> builder, Optional<Double> value) {
            value.filter(McpDecorators::isPositiveAndLessThanOne)
                    .orElseThrow(() -> new IllegalArgumentException("Cost priority must be in range [0, 1]"));
        }
    }

    /**
     * Enforce annotation priority value between 0 and 1.
     */
    static class AnnotationsPriorityDecorator
            implements Prototype.OptionDecorator<McpAnnotations.BuilderBase<?, ?>, Optional<Double>> {
        @Override
        public void decorate(McpAnnotations.BuilderBase<?, ?> builder, Optional<Double> value) {
            value.filter(priority -> !McpDecorators.isPositiveAndLessThanOne(priority))
                    .ifPresent(priority -> {
                        throw new IllegalArgumentException("Annotation priority must be in range [0, 1]");
                    });
        }
    }

    /**
     * Enforce a valid HTTP(S) URL or data URI for an icon source.
     */
    static class IconSourceDecorator implements Prototype.OptionDecorator<McpIcon.BuilderBase<?, ?>, String> {
        private static final String INVALID_ICON_SOURCE = "Icon source must be a valid HTTP(S) URL or data URI";
        private static final String BASE64_MARKER = ";base64";

        @Override
        public void decorate(McpIcon.BuilderBase<?, ?> builder, String value) {
            if (value.isBlank()) {
                throw new IllegalArgumentException(INVALID_ICON_SOURCE);
            }

            URI source;
            try {
                source = URI.create(value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(INVALID_ICON_SOURCE, e);
            }

            String scheme = source.getScheme();
            if (scheme == null) {
                throw new IllegalArgumentException(INVALID_ICON_SOURCE);
            }

            switch (scheme.toLowerCase(Locale.ROOT)) {
                case "http", "https" -> {
                    if (source.getHost() == null) {
                        throw new IllegalArgumentException(INVALID_ICON_SOURCE);
                    }
                }
                case "data" -> {
                    String data = source.getRawSchemeSpecificPart();
                    int separator = data.indexOf(',');
                    if (!source.isOpaque() || separator < 0 || separator == data.length() - 1) {
                        throw new IllegalArgumentException(INVALID_ICON_SOURCE);
                    }
                    String metadata = data.substring(0, separator);
                    int base64Offset = metadata.length() - BASE64_MARKER.length();
                    if (base64Offset < 0
                            || !metadata.regionMatches(true, base64Offset, BASE64_MARKER, 0, BASE64_MARKER.length())) {
                        throw new IllegalArgumentException(INVALID_ICON_SOURCE);
                    }
                    String mediaType = metadata.substring(0, base64Offset);
                    if (!isValidMediaType(mediaType)) {
                        throw new IllegalArgumentException(INVALID_ICON_SOURCE);
                    }
                    try {
                        Base64.getDecoder().decode(decodePercentEncoded(data.substring(separator + 1)));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(INVALID_ICON_SOURCE, e);
                    }
                }
                default -> throw new IllegalArgumentException(INVALID_ICON_SOURCE);
            }
        }

        private static boolean isValidMediaType(String value) {
            if (value.isEmpty()) {
                return true;
            }

            int firstParameter = value.indexOf(';');
            int mediaTypeEnd = firstParameter == -1 ? value.length() : firstParameter;
            if (mediaTypeEnd > 0) {
                int slash = value.indexOf('/');
                if (slash <= 0 || slash >= mediaTypeEnd - 1) {
                    return false;
                }
                int secondSlash = value.indexOf('/', slash + 1);
                if ((secondSlash >= 0 && secondSlash < mediaTypeEnd)
                        || !isMimeToken(value, 0, slash)
                        || !isMimeToken(value, slash + 1, mediaTypeEnd)) {
                    return false;
                }
            }

            for (int offset = mediaTypeEnd; offset < value.length();) {
                if (value.charAt(offset) != ';') {
                    return false;
                }
                int end = value.indexOf(';', offset + 1);
                end = end == -1 ? value.length() : end;
                int equals = value.indexOf('=', offset + 1);
                if (equals < offset + 2 || equals >= end - 1
                        || !isMimeToken(value, offset + 1, equals)
                        || !isParameterValue(value, equals + 1, end)) {
                    return false;
                }
                int secondEquals = value.indexOf('=', equals + 1);
                if (secondEquals >= 0 && secondEquals < end) {
                    return false;
                }
                offset = end;
            }
            return true;
        }

        private static boolean isMimeToken(String value, int start, int end) {
            if (start == end) {
                return false;
            }
            for (int i = start; i < end; i++) {
                char character = value.charAt(i);
                if (character == '%') {
                    if (i + 2 >= end) {
                        return false;
                    }
                    int high = Character.digit(value.charAt(++i), 16);
                    int low = Character.digit(value.charAt(++i), 16);
                    if (high == -1 || low == -1) {
                        return false;
                    }
                    character = (char) ((high << 4) | low);
                }
                if (!isMimeTokenCharacter(character)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean isParameterValue(String value, int start, int end) {
            if (start == end) {
                return false;
            }
            for (int i = start; i < end; i++) {
                char character = value.charAt(i);
                if (character != '%') {
                    if (!isMimeTokenCharacter(character)) {
                        return false;
                    }
                    continue;
                }
                if (i + 2 >= end) {
                    return false;
                }
                int high = Character.digit(value.charAt(++i), 16);
                int low = Character.digit(value.charAt(++i), 16);
                if (high == -1 || low == -1) {
                    return false;
                }
                int decoded = (high << 4) | low;
                if (decoded < 0x20 || decoded > 0x7e) {
                    return false;
                }
            }
            return true;
        }

        private static boolean isMimeTokenCharacter(char character) {
            if (character < 0x21 || character > 0x7e) {
                return false;
            }
            return switch (character) {
                case '(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=' -> false;
                default -> true;
            };
        }

        private static String decodePercentEncoded(String value) {
            int firstEscape = value.indexOf('%');
            if (firstEscape == -1) {
                return value;
            }
            StringBuilder decoded = new StringBuilder(value.length());
            decoded.append(value, 0, firstEscape);
            for (int i = firstEscape; i < value.length(); i++) {
                char character = value.charAt(i);
                if (character != '%') {
                    decoded.append(character);
                    continue;
                }
                if (i + 2 >= value.length()) {
                    throw new IllegalArgumentException(INVALID_ICON_SOURCE);
                }
                int high = Character.digit(value.charAt(++i), 16);
                int low = Character.digit(value.charAt(++i), 16);
                if (high == -1 || low == -1) {
                    throw new IllegalArgumentException(INVALID_ICON_SOURCE);
                }
                decoded.append((char) ((high << 4) | low));
            }
            return decoded.toString();
        }
    }

    /**
     * The URI scheme must be {@code file} when creating an MCP root.
     */
    static class RootUriDecorator implements Prototype.OptionDecorator<McpRoot.BuilderBase<?, ?>, URI> {
        @Override
        public void decorate(McpRoot.BuilderBase<?, ?> builder, URI uri) {
            if (!uri.getScheme().equals("file")) {
                throw new McpRootException("Root URI scheme must be file");
            }
        }
    }

    /**
     * Enforce an absolute URI for URL mode elicitation.
     */
    static class ElicitationUrlDecorator implements Prototype.OptionDecorator<McpElicitationUrlRequest.BuilderBase<?, ?>, URI> {
        @Override
        public void decorate(McpElicitationUrlRequest.BuilderBase<?, ?> builder, URI value) {
            if (!value.isAbsolute()) {
                throw new McpElicitationException("Elicitation URL must be an absolute URI");
            }
        }
    }

    /**
     * Enforce a non-blank URL mode elicitation identifier.
     */
    static class ElicitationIdDecorator implements Prototype.OptionDecorator<McpElicitationUrlRequest.BuilderBase<?, ?>, String> {
        @Override
        public void decorate(McpElicitationUrlRequest.BuilderBase<?, ?> builder, String value) {
            if (value.isBlank()) {
                throw new McpElicitationException("Elicitation ID must not be blank");
            }
        }
    }

    /**
     * Number of suggestions must not exceed 100 items.
     */
    static class CompletionValuesDecorator implements Prototype.OptionDecorator<McpCompletionResult.BuilderBase<?, ?>, String> {
        @Override
        public void decorate(McpCompletionResult.BuilderBase<?, ?> builder, String value) {
        }

        @Override
        public void decorateSetList(McpCompletionResult.BuilderBase<?, ?> builder, List<String> values) {
            lessThan100Items(values);
        }

        @Override
        public void decorateAddList(McpCompletionResult.BuilderBase<?, ?> builder, List<String> values) {
            lessThan100Items(values);
        }

        @Override
        public void decorateSetSet(McpCompletionResult.BuilderBase<?, ?> builder, Set<String> values) {
            lessThan100Items(values);
        }

        @Override
        public void decorateAddSet(McpCompletionResult.BuilderBase<?, ?> builder, Set<String> values) {
            lessThan100Items(values);
        }

        private void lessThan100Items(Collection<String> values) {
            if (values.size() > 100) {
                throw new IllegalArgumentException("Completion values must be less than 100");
            }
        }
    }

    static class PositiveValueDecorator implements Prototype.OptionDecorator<McpServerConfig.BuilderBase<?, ?>, Integer> {
        @Override
        public void decorate(McpServerConfig.BuilderBase<?, ?> builder, Integer value) {
            if (value < 0) {
                throw new IllegalArgumentException("value must be greater than zero");
            }
        }
    }

    static class SamplingToolIterationsDecorator
            implements Prototype.OptionDecorator<McpServerConfig.BuilderBase<?, ?>, Integer> {
        @Override
        public void decorate(McpServerConfig.BuilderBase<?, ?> builder, Integer value) {
            if (value <= 0) {
                throw new IllegalArgumentException("Maximum sampling tool iterations must be greater than zero");
            }
        }
    }
}
