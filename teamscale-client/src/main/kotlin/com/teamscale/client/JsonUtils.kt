package com.teamscale.client

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import java.io.File
import java.io.IOException

/**
 * Utility object for serializing and deserializing JSON using Jackson.
 */
object JsonUtils {

	/**
	 * Jackson ObjectMapper that is used for serializing and deserializing JSON objects. The visibility settings of the
	 * OBJECT_MAPPER are configured to include all fields when serializing or deserializing objects, regardless of their
	 * visibility modifiers (public, private, etc.).
	 */
	@JvmStatic val OBJECT_MAPPER: ObjectMapper =
		JsonMapper.builder()
			.visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
			.visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.serializationInclusion(JsonInclude.Include.NON_NULL)
			.build()

	/**
	 * Creates a new instance of {@link JsonFactory} using the default {@link ObjectMapper}.
	 */
	@JvmStatic fun createFactory() = JsonFactory(OBJECT_MAPPER)

	/**
	 * Deserializes a JSON string into an object of the given class.
	 */
	@Throws(JsonProcessingException::class, JsonMappingException::class)
	inline fun <reified T> String.deserialize(): T =
		OBJECT_MAPPER.readValue(this, T::class.java)

	/**
	 * Deserializes the contents of the given file into an object of the given class.
	 */
	@Throws(IOException::class, StreamReadException::class, DatabindException::class)
	inline fun <reified T> File.deserialize(): T =
		OBJECT_MAPPER.readValue(this, T::class.java)

	// ToDo: Remove when System tests are in Kotlin
	@JvmStatic fun <T> String.deserialize(clazz: Class<*>) =
		OBJECT_MAPPER.readValue(this, clazz) as T

	// ToDo: Remove when System tests are in Kotlin
	fun <T> File.deserializeAsArray(clazz: Class<T>): ArrayList<T> =
		OBJECT_MAPPER.readValue(
			this,
			OBJECT_MAPPER.typeFactory.constructCollectionLikeType(ArrayList::class.java, clazz)
		)

	inline fun <reified T> File.deserializeAsArray(): ArrayList<T> =
		OBJECT_MAPPER.readValue(
			this,
			OBJECT_MAPPER.typeFactory.constructCollectionLikeType(ArrayList::class.java, T::class.java)
		)

	/**
	 * Deserializes a JSON string into a list of objects of the given class.
	 * @param [T] The type of the objects in the list.
	 */
	inline fun <reified T> String.deserializeAsList(): List<T> =
		OBJECT_MAPPER.readValue(
			this,
			OBJECT_MAPPER.typeFactory.constructCollectionLikeType(List::class.java, T::class.java)
		)

	/**
	 * Serializes an object into its JSON representation.
	 * @throws JsonProcessingException if the serialization fails
	 */
	@JvmStatic fun Any.serialize(): String =
		OBJECT_MAPPER.writeValueAsString(this)

	/**
	 * Serializes an object to a file with pretty printing enabled.
	 */
	fun <T> T.serializeWriteToFile(file: File) {
		OBJECT_MAPPER.writer().withDefaultPrettyPrinter().writeValue(file, this)
	}
}