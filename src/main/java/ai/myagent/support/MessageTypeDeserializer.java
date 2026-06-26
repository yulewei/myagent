package ai.myagent.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.ai.chat.messages.MessageType;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author yulewei
 * @since 2026/6/23
 */
public class MessageTypeDeserializer extends StdDeserializer<MessageType> {

    public MessageTypeDeserializer() {
        super(MessageType.class);
    }

    @Override
    public MessageType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null) {
            return null;
        }
        return Arrays.stream(MessageType.values())
                .filter(type -> type.getValue().equalsIgnoreCase(value))
                .findFirst().orElse(null);
    }
}
