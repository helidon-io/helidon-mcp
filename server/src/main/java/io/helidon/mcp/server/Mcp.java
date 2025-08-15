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

package io.helidon.mcp.server;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This interface contains a set of annotations to define an MCP declarative server.
 */
public final class Mcp {

    /**
     * Annotation to define a MCP server.
     */
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface Server {
        /**
         * Name of the server.
         *
         * @return server name
         */
        String value() default "helidon-mcp-server";
    }

    /**
     * Annotation to describe an MCP component such as {@link io.helidon.mcp.server.Mcp.Tool},
     * {@link io.helidon.mcp.server.Mcp.Prompt} and {@link io.helidon.mcp.server.Mcp.Resource}.
     */
    @Target({TYPE, METHOD, FIELD, PARAMETER})
    @Retention(RUNTIME)
    public @interface Description {
        /**
         * Component description.
         *
         * @return description
         */
        String value();
    }

    /**
     * Annotation to define the {@link io.helidon.mcp.server.Mcp.Server} version.
     */
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface Version {
        /**
         * Version of the server.
         *
         * @return server version
         */
        String value();
    }

    /**
     * Annotation to define the {@link io.helidon.mcp.server.Mcp.Server} version.
     */
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface Path {
        /**
         * Path of the server.
         *
         * @return server path
         */
        String value();
    }

    /**
     * Annotation to define an MCP Tool.
     */
    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface Tool {
        /**
         * Description of the tool.
         *
         * @return tool name
         */
        String value();
    }

    /**
     * Annotation to define an MCP Prompt.
     */
    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface Prompt {
        /**
         * Description of the Prompt.
         *
         * @return name
         */
        String value();
    }

    /**
     * Annotation to define an MCP resource.
     */
    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface Resource {
        /**
         * URI of the resource.
         *
         * @return name
         */
        String uri();

        /**
         * Media type of the resource.
         *
         * @return media type
         */
        String mediaType();

        /**
         * Description of the resource.
         *
         * @return description
         */
        String description();
    }

    /**
     * Annotation to define a completion for {@link io.helidon.mcp.server.Mcp.Prompt} name
     * and {@link io.helidon.mcp.server.Mcp.Resource} template uri.
     */
    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface Completion {
        /**
         * Resource URI template or Prompt name.
         *
         * @return uri or prompt name
         */
        String value();
    }

    /**
     * Annotation to define a class used as {@link io.helidon.mcp.server.Mcp.Tool} input.
     */
    public @interface JsonSchema {
        /**
         * Json Schema main description. Note that {@link io.helidon.mcp.server.Mcp.Description}
         * can be used instead.
         *
         * @return description
         */
        String value() default "";
    }

    /**
     * Annotation to define prompt content {@link io.helidon.mcp.server.McpRole}.
     */
    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface Role {
        /**
         * Role with {@code ASSISTANT} as default value.
         *
         * @return role
         */
        io.helidon.mcp.server.McpRole value() default io.helidon.mcp.server.McpRole.ASSISTANT;
    }

    /**
     * Annotation to define a Prompt, Resource or Tool name.
     */
    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface Name {
        /**
         * Prompt, Resource or Tool name.
         *
         * @return name
         */
        String value();
    }
}
