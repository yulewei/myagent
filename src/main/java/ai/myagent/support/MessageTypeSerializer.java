package ai.myagent.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.ai.chat.messages.MessageType;

import java.io.IOException;

/**
 * @author yulewei
 * @since 2026/6/23
 */
public class MessageTypeSerializer extends StdSerializer<MessageType> {

    public MessageTypeSerializer() {
        super(MessageType.class);
    }

    @Override
    public void serialize(MessageType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getValue());
    }
}
