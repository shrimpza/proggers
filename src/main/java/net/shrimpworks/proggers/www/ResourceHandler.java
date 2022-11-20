package net.shrimpworks.proggers.www;

import java.io.IOException;
import java.io.InputStream;

import com.sun.net.httpserver.HttpExchange;

public class ResourceHandler extends Handler {

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
			// TODO log
			e.printStackTrace();
			respondPlain(exchange, 500, "Something went wrong");
		}
	}
}
