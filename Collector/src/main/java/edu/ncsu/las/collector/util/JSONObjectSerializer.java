package edu.ncsu.las.collector.util;

import java.io.IOException;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JSONObjectSerializer extends JsonSerializer<JSONObject> {
  	@Override
	public void serialize(JSONObject value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("value",value.toString());
        jgen.writeEndObject();
	}
}