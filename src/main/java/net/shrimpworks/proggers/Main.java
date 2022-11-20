package net.shrimpworks.proggers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.shrimpworks.proggers.service.ProgressService;
import net.shrimpworks.proggers.service.ProgressStore;
import net.shrimpworks.proggers.www.WebService;

public class Main {

	private static final String DEFAULT_BIND_HOST = "127.0.0.1";
	private static final int DEFAULT_PORT = 8088;
	private static final String DATA_PATH = "data"; // assumes to be in PWD

	public static void main(String[] args) throws IOException {
		Path dataPath = Paths.get(DATA_PATH).toAbsolutePath();

		final ProgressStore store = new ProgressStore.FileStore(dataPath);
		final ProgressService service = new ProgressService(store);

		final WebService webService = new WebService(service, new InetSocketAddress(DEFAULT_BIND_HOST, DEFAULT_PORT));

		System.out.printf("Started and listening on port %d with data store in %s%n", DEFAULT_PORT, dataPath);

		Runtime.getRuntime().addShutdownHook(new Thread(webService::close));
	}
}
