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
package io.helidon.demo.mcp.client;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface WeatherAiChat {

    /**
     * Weather AI chat.
     *
     * @param question the weather question
     * @return the response
     */
    @UserMessage("You are a weather journalist. {{question}}")
    String weather(@V("question") String question);
}
