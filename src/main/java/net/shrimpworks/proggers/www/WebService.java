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

	private final int TCP_ACCEPT_BACKLOG = 5;

	private final ExecutorService executor;

	private final HttpServer httpServer;

	public WebService(ProgressService progress, InetSocketAddress listenAddress) throws IOException {
		this.executor = Executors.newCachedThreadPool();

		this.httpServer = HttpServer.create(listenAddress, TCP_ACCEPT_BACKLOG);
		this.httpServer.setExecutor(this.executor);

		final ResourceHandler indexHandler = new ResourceHandler("index.html");

		// set up static content handlers
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("resources.list")))) {
			String l;
			while ((l = reader.readLine()) != null) this.httpServer.createContext(l.trim(), new ResourceHandler(l.trim()));
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
