package io.helidon.mcp.server;

import io.helidon.common.media.type.MediaType;

final class McpImageContentImpl implements McpImageContent {
    private final byte[] data;
    private final MediaType type;

    McpImageContentImpl(byte[] data, MediaType type) {
        this.data = data;
        this.type = type;
    }

    @Override
    public byte[] data() {
        return data;
    }

    @Override
    public MediaType mediaType() {
        return type;
    }

    @Override
    public ContentType type() {
        return ContentType.IMAGE;
    }
}
