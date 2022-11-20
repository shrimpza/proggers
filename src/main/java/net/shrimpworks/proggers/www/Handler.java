package net.shrimpworks.proggers.www;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import net.shrimpworks.proggers.JSON;

public abstract class Handler implements HttpHandler {

	protected static final Pattern QUERY_PARAMS = Pattern.compile("[?&]?([^&=]+)=([^&=]+)");

	protected Map<String, String> queryParams(HttpExchange exchange) {
		Map<String, String> params = new HashMap<>();
		if (exchange.getRequestURI().getQuery() != null && !exchange.getRequestURI().getQuery().isBlank()) {
			Matcher paramMatcher = QUERY_PARAMS.matcher(exchange.getRequestURI().getQuery());
			while (paramMatcher.find()) params.put(paramMatcher.group(1), paramMatcher.group(2));
		}
		return params;
	}

	protected void respondPlain(HttpExchange exchange, int status, String message) {
		try {
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			if (message == null || message.isEmpty()) {
				exchange.sendResponseHeaders(status, -1);
			} else {
				exchange.sendResponseHeaders(status, message.length());
				exchange.getResponseBody().write(message.getBytes(StandardCharsets.UTF_8));
				exchange.getResponseBody().flush();
				exchange.getResponseBody().close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			// TODO log
		}
	}

	protected void respondJson(HttpExchange exchange, int status, Object message) {
		try {
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			if (message == null) {
				exchange.sendResponseHeaders(status, -1);
			} else {
				String out = JSON.toString(message);
				exchange.sendResponseHeaders(status, out.length());
				exchange.getResponseBody().write(out.getBytes(StandardCharsets.UTF_8));
				exchange.getResponseBody().flush();
				exchange.getResponseBody().close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			// TODO log
		}
	}

	protected void respondStream(HttpExchange exchange, String filename, InputStream stream, int status) {
		try {
			// hax maybe
			String contentType = "text/plain";
			if (filename.endsWith("html")) contentType = "text/html";
			else if (filename.endsWith("js")) contentType = "text/javascript";
			else if (filename.endsWith("css")) contentType = "text/css";
			else if (filename.endsWith("jpg")) contentType = "image/jpeg";
			else if (filename.endsWith("jpeg")) contentType = "image/jpeg";
			else if (filename.endsWith("png")) contentType = "image/png";
			exchange.getResponseHeaders().add("Content-Type", contentType);

			if (stream == null || stream.available() == 0) {
				exchange.sendResponseHeaders(status, -1);
			} else {
				// FIXME assumes all bytes are ready in the stream
				exchange.sendResponseHeaders(status, stream.available());
				stream.transferTo(exchange.getResponseBody());
				exchange.getResponseBody().flush();
				exchange.getResponseBody().close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			// TODO log
		}
	}

}
