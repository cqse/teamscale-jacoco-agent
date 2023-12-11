package com.teamscale.report.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility class for serializing and deserializing JSON using Jackson.
 */
public class JsonUtils {

	/**
	 * Jackson ObjectMapper that is used for serializing and deserializing JSON objects. The visibility settings of the
	 * OBJECT_MAPPER are configured to include all fields when serializing or deserializing objects, regardless of their
	 * visibility modifiers (public, private, etc.).
	 */
	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
			.visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
			.visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
			.serializationInclusion(JsonInclude.Include.NON_NULL)
			.build();

	/**
	 * Creates a new instance of {@link JsonFactory} using the default {@link ObjectMapper}.
	 */
	public static JsonFactory createFactory() {
		return new JsonFactory(OBJECT_MAPPER);
	}

	/**
	 * Deserializes a JSON string into an object of the given class.
	 */
	public static <T> T deserialize(String json, Class<T> clazz) throws JsonProcessingException {
		return OBJECT_MAPPER.readValue(json, clazz);
	}

	/**
	 * Deserializes the contents of the given file into an object of the given class.
	 */
	public static <T> T deserializeFile(File file, Class<T> clazz) throws IOException {
		return OBJECT_MAPPER.readValue(file, clazz);
	}

	/**
	 * Deserializes a JSON string into a list of objects of the given class.
	 */
	public static <T> List<T> deserializeList(String json, Class<T> elementClass) throws JsonProcessingException {
		return OBJECT_MAPPER.readValue(json,
				OBJECT_MAPPER.getTypeFactory().constructCollectionLikeType(List.class, elementClass));
	}

	/**
	 * Serializes an object into its JSON representation.
	 */
	public static String serialize(Object value) throws JsonProcessingException {
		return OBJECT_MAPPER.writeValueAsString(value);
	}

	/**
	 * Serializes an object to a file with pretty printing enabled.
	 */
	public static <T> void serializeToFile(File file, T value) throws IOException {
		OBJECT_MAPPER.writer().withDefaultPrettyPrinter().writeValue(file, value);
	}
}
