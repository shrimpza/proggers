package net.shrimpworks.proggers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;

import net.shrimpworks.proggers.service.ProgressService;
import net.shrimpworks.proggers.service.ProgressStore;
import net.shrimpworks.proggers.www.WebService;

public class Main {

	public static void main(String[] args) throws IOException {
		final ProgressStore store = new ProgressStore.FileStore(Paths.get("data"));
		final ProgressService service = new ProgressService(store);

		final WebService webService = new WebService(service, new InetSocketAddress("127.0.0.1", 8088));

		Runtime.getRuntime().addShutdownHook(new Thread(webService::close));
	}
}
