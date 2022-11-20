package net.shrimpworks.proggers.www;

import java.io.IOException;
import java.io.InputStream;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceHandler extends Handler {
	private static final Logger log = LoggerFactory.getLogger(ResourceHandler.class);

	// rather than a generic handler serving resources based on the requested URL, we'll bind instances to specific resource files
	private final String resourcePath;

	public ResourceHandler(String resourcePath) {
		if (resourcePath.startsWith("/")) this.resourcePath = resourcePath.substring(1);
		else this.resourcePath = resourcePath;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			InputStream resource = getClass().getResourceAsStream(resourcePath);

			if (resource == null) {
				respondStream(exchange, resourcePath, null, 404);
			} else {
				respondStream(exchange, resourcePath, resource, 200);
			}
		} catch (Exception e) {
			log.warn("Exception while handling static resource request: {}", e, e);
			respondPlain(exchange, 500, "Something went wrong");
		} finally {
			logHit(exchange);
		}
	}
}
