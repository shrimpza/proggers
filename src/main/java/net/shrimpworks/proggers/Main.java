package net.shrimpworks.proggers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shrimpworks.proggers.service.ProgressService;
import net.shrimpworks.proggers.service.ProgressStore;
import net.shrimpworks.proggers.www.WebService;

public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	private static final String DEFAULT_BIND_HOST = "127.0.0.1";
	private static final int DEFAULT_PORT = 8088;
	private static final String DATA_PATH = "data"; // assumes to be in PWD

	public static void main(String[] args) throws IOException {
		Path dataPath = Paths.get(DATA_PATH).toAbsolutePath();

		final ProgressStore store = new ProgressStore.FileStore(dataPath);
		final ProgressService service = new ProgressService(store);

		final WebService webService = new WebService(service, new InetSocketAddress(DEFAULT_BIND_HOST, DEFAULT_PORT));

		log.info("Started and listening on port {} with data store in {}", DEFAULT_PORT, dataPath);

		Runtime.getRuntime().addShutdownHook(new Thread(webService::close));
	}
}
