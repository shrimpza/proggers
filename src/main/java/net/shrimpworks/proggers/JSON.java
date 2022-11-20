package net.shrimpworks.proggers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JSON {

	private static final ObjectMapper MAPPER;

	static {
		MAPPER = JsonMapper.builder(new JsonFactory())
						   .addModule(new JavaTimeModule())
						   .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
						   .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
						   .serializationInclusion(JsonInclude.Include.NON_NULL)
						   .build();
	}

	public static byte[] toBytes(Object object) throws IOException {
		return MAPPER.writeValueAsBytes(object);
	}

	public static String toString(Object object) throws IOException {
		return MAPPER.writeValueAsString(object);
	}

	public static <T> T fromString(String yaml, Class<T> type) throws IOException {
		return MAPPER.readValue(yaml, type);
	}

	public static <T> T fromString(String yaml, TypeReference<T> type) throws IOException {
		return MAPPER.readValue(yaml, type);
	}

	public static <T> T fromFile(Path path, Class<T> type) throws IOException {
		try {
			return MAPPER.readValue(Files.newInputStream(path), type);
		} catch (Exception e) {
			throw new IOException("Failed to read file " + path.toString(), e);
		}
	}

	public static <T> T fromFile(Path path, TypeReference<T> type) throws IOException {
		try {
			return MAPPER.readValue(Files.newInputStream(path), type);
		} catch (Exception e) {
			throw new IOException("Failed to read file " + path.toString(), e);
		}
	}

}
