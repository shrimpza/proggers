package net.shrimpworks.proggers.www;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import net.shrimpworks.proggers.service.ProgressService;

public class WebService implements Closeable {

	private static final int TCP_ACCEPT_BACKLOG = 5;

	private final ExecutorService executor;

	private final HttpServer httpServer;

	public WebService(ProgressService progress, InetSocketAddress listenAddress) throws IOException {
		this.executor = Executors.newCachedThreadPool();

		this.httpServer = HttpServer.create(listenAddress, TCP_ACCEPT_BACKLOG);
		this.httpServer.setExecutor(this.executor);

		ResourceHandler indexHandler = null;

		// set up static content handlers
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("resources.list")))) {
			String l;
			while ((l = reader.readLine()) != null) {
				if (l.isBlank() || l.startsWith("#")) continue;

				// splits into path, etag, max-age
				String[] res = l.split("\\s+");
				ResourceHandler handler = new ResourceHandler(res[0].trim(), res[1], ProgressService.stringToDuration(res[2]));

				this.httpServer.createContext(res[0].trim(), handler);

				// any non-matched URLs will hit this, we serve the index page from here
				if (res[0].equals("/index.html")) indexHandler = handler;
			}
		}

		// this will process API requests
		this.httpServer.createContext("/progress", new ProgressHandler(progress));

		// any non-matched URLs will hit this, we serve the index page from here
		this.httpServer.createContext("/", indexHandler);

		this.httpServer.start();
	}

	@Override
	public void close() {
		this.httpServer.stop(0);
		this.executor.shutdownNow();
	}

}
