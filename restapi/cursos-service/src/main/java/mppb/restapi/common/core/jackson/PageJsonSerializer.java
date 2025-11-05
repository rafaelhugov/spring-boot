package mppb.restapi.common.core.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.domain.Page;

import java.io.IOException;

@JsonComponent
public class PageJsonSerializer extends JsonSerializer<Page<?>> {

	@Override
	public void serialize(Page<?> page, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeStartObject();

		// Esta linha faz respeitar a serializacao do jsonview.
		// normal, que não respeita jsonview: gen.writeObjectField("content", page.getContent());
		serializers.defaultSerializeField("content", page.getContent(), gen);

		gen.writeNumberField("size", page.getSize());
		gen.writeNumberField("totalElements", page.getTotalElements());
		gen.writeNumberField("totalPages", page.getTotalPages());
		gen.writeNumberField("number", page.getNumber());
		gen.writeEndObject();
	}

}
