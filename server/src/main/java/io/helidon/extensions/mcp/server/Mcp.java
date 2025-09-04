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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This interface contains a set of annotations to define an MCP server.
 */
public final class Mcp {

    /**
     * Annotation to define an MCP server. An MCP Server aggregates several MCP
     * components like tools, prompts, resources and completions.
     *
     * <p>The primary components include:</p>
     * <ul>
     *   <li>
     *       {@link io.helidon.extensions.mcp.server.Mcp.Tool} -
     *       Tool is a function that computes a set of inputs and return a result. Mcp server uses tools to
     *       interact with the outside world to reach real time data through API calls, access to databases
     *       or performing any kind of computation.
     *   </li>
     *   <li>
     *       {@link io.helidon.extensions.mcp.server.Mcp.Prompt} -
     *       Prompt is a set of instruction using parameters. An MCP Prompt is often referred as a template.
     *       Associated with a {@link io.helidon.extensions.mcp.server.McpRole}, it is usually used by the
     *       client application to exercise the MCP server.
     *   </li>
     *   <li>
     *       {@link io.helidon.extensions.mcp.server.Mcp.Resource} -
     *       Resource shares data that provides context to LLM. Identified by a URI, a resource can be, as an example,
     *       a file or a web page. To create a {@code Resource Template}, the same annotation can be used.
     *       A {@code Resource Template} has a URI that contains parameters like {@code {parameter}}.
     *   </li>
     *   <li>
     *      {@link io.helidon.extensions.mcp.server.Mcp.Completion} -
     *      Completion helps client application to fulfilled {@code Prompt} arguments or {@code Resource Template} parameters.
     *      This way, the server can suggest where are resources located and which arguments can be used.
     *   </li>
     * </ul>
     * <p>The MCP server can be configured using the following annotations:</p>
     * <ul>
     *     <li>
     *         {@link io.helidon.extensions.mcp.server.Mcp.Version} -
     *         Set the MCP server version. It will be communicated to MCP client when connecting to this server.
     *     </li>
     *     <li>
     *         {@link io.helidon.extensions.mcp.server.Mcp.Path} -
     *         Set the path that an MCP server will server request for. Paths are relative, the base URI is served
     *         by Helidon Webserver and can be configured accordingly.
     *     </li>
     * </ul>
     */
    @Target(TYPE)
    @Retention(CLASS)
    public @interface Server {
        /**
         * Name of the server.
         *
         * @return server name
         */
        String value() default "mcp-server";
    }

    /**
     * Annotation to describe a prompt argument.
     */
    @Target({TYPE, METHOD, FIELD, PARAMETER})
    @Retention(RUNTIME)
    public @interface Description {
        /**
         * Prompt argument description.
         *
         * @return description
         */
        String value();
    }

    /**
     * Annotation to define the {@link io.helidon.extensions.mcp.server.Mcp.Server} version.
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
     * Annotation to define the {@link io.helidon.extensions.mcp.server.Mcp.Server} path.
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
     * A tool is a none static method and must be located in a class annotated with
     * {@link io.helidon.extensions.mcp.server.Mcp.Server}. This way, the tool is
     * automatically registered to the server.
     *
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
     * A prompt is a none static method and must be located in a class annotated with
     * {@link io.helidon.extensions.mcp.server.Mcp.Server}. This way, the prompt is
     * automatically registered to the server.
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
     * A resource is a none static method and must be located in a class annotated with
     * {@link io.helidon.extensions.mcp.server.Mcp.Server}. This way, the resource is automatically registered to the server.
     *
     * <p>This annotation supports two kinds of Resource:</p>
     * <ul>
     *     <li>
     *         {@code Regular Resource} where the resource {@link java.net.URI} points to an MCP resource such as
     *         files, web page or any internal or external data.
     *     </li>
     *     <li>
     *         {@code Resource Template} where the resource {@code URI} contains parameter(s). Those templates are meant to be
     *         used by client application in order to discover regular resources. A resource template can not be read and
     *         provide resource content, they are hints to find regular resources.
     *     </li>
     * </ul>
     */
    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface Resource {
        /**
         * URI of the resource. Use parameter within curly bracket, like {@code {parameter}}, to create
         * a {@code Resource Template}.
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
     * Annotation to define a completion for {@link io.helidon.extensions.mcp.server.Mcp.Prompt}
     * argument and {@link io.helidon.extensions.mcp.server.Mcp.Resource} template uri.
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
     * Annotation to define JSON Schema of a type used as a tool parameter.
     */
    public @interface JsonSchema {
        /**
         * Json Schema as a {@code String}.
         *
         * @return Json schema
         */
        String value();
    }

    /**
     * Annotation to define a {@code Prompt}, {@code Resource} or {@code Tool} name.
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

    /**
     * Annotation to define prompt content {@link io.helidon.extensions.mcp.server.McpRole}
     * when used on a prompt that return a {@code String}. Otherwise, this annotation is ignored.
     */
    @Target(METHOD)
    @Retention(CLASS)
    public @interface Role {
        /**
         * Role with {@code ASSISTANT} as default value.
         *
         * @return role
         */
        McpRole value() default McpRole.ASSISTANT;
    }

    /**
     * Annotation to define {@code Tools} page size.
     * <p>
     * Pagination occurs when client uses MCP listing methods. It enables the server to return results in smaller,
     * manageable chunks rather than delivering the entire dataset at once. This class maintains a map of pages, where each key
     * represents a unique cursor associated with a specific page. Each page also contains a cursor pointing to the next page in
     * the sequence.
     */
    @Target(TYPE)
    @Retention(CLASS)
    public @interface ToolsPageSize {
        /**
         * Configure page size for {@code Tool} list.
         *
         * @return page size
         */
        int value() default McpPagination.DEFAULT_PAGE_SIZE;
    }

    /**
     * Annotation to define {@code Prompts} page size.
     * <p>
     * Pagination occurs when client uses MCP listing methods. It enables the server to return results in smaller,
     * manageable chunks rather than delivering the entire dataset at once. This class maintains a map of pages, where each key
     * represents a unique cursor associated with a specific page. Each page also contains a cursor pointing to the next page in
     * the sequence.
     */
    @Target(TYPE)
    @Retention(CLASS)
    public @interface PromptsPageSize {
        /**
         * Configure page size for {@code Prompt} list.
         *
         * @return page size
         */
        int value() default McpPagination.DEFAULT_PAGE_SIZE;
    }

    /**
     * Annotation to define {@code Resources} page size.
     * <p>
     * Pagination occurs when client uses MCP listing methods. It enables the server to return results in smaller,
     * manageable chunks rather than delivering the entire dataset at once. This class maintains a map of pages, where each key
     * represents a unique cursor associated with a specific page. Each page also contains a cursor pointing to the next page in
     * the sequence.
     */
    @Target(TYPE)
    @Retention(CLASS)
    public @interface ResourcesPageSize {
        /**
         * Configure page size for {@code Resource} list.
         *
         * @return page size
         */
        int value() default McpPagination.DEFAULT_PAGE_SIZE;
    }

    /**
     * Annotation to define {@code Resource Templates} page size.
     * <p>
     * Pagination occurs when client uses MCP listing methods. It enables the server to return results in smaller,
     * manageable chunks rather than delivering the entire dataset at once. This class maintains a map of pages, where each key
     * represents a unique cursor associated with a specific page. Each page also contains a cursor pointing to the next page in
     * the sequence.
     */
    @Target(TYPE)
    @Retention(CLASS)
    public @interface ResourceTemplatesPageSize {
        /**
         * Configure page size for {@code Resource Template} list.
         *
         * @return page size
         */
        int value() default McpPagination.DEFAULT_PAGE_SIZE;
    }
}
