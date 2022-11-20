package net.shrimpworks.proggers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.type.TypeReference;

import net.shrimpworks.proggers.service.ProgressService;
import net.shrimpworks.proggers.service.ProgressStore;
import net.shrimpworks.proggers.www.ProgressHandler;
import net.shrimpworks.proggers.www.WebService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebServiceTest {

	private ProgressService ps;
	private WebService ws;

	private final HttpClient client = HttpClient.newBuilder().build();
	private final HttpRequest.Builder builder = HttpRequest.newBuilder()
														   .uri(URI.create("http://localhost:58780/progress"));

	@BeforeEach
	public void before() throws IOException {
		ps = new ProgressService(new ProgressStore.MemoryStore());
		ws = new WebService(ps, new InetSocketAddress("127.0.0.1", 58780));
	}

	@AfterEach
	public void after() {
		ws.close();
	}

	@Test
	public void sendProgressOK() throws IOException, InterruptedException {
		HttpRequest rq = builder
			.uri(URI.create("http://localhost:58780/progress/testing/lol"))
			.POST(HttpRequest.BodyPublishers.ofString("max=9001&progress=5800&ttl=19m"))
			.build();

		HttpResponse<String> send = client.send(rq, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, send.statusCode());

		// try to get it
		rq = builder
			.uri(URI.create("http://localhost:58780/progress/testing"))
			.GET()
			.build();
		HttpResponse<String> recv = client.send(rq, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, recv.statusCode());
		Set<ProgressHandler.SubscriberUpdate> progresses = JSON.fromString(recv.body(), new TypeReference<>() {});
		assertEquals(1, progresses.size());
		assertEquals("lol", progresses.stream().findFirst().get().updated.name);
	}

	@Test
	public void itsNotFatItsJustBigBoned() throws IOException, InterruptedException {
		HttpRequest rq = builder
			.POST(HttpRequest.BodyPublishers.ofString("long body".repeat(100)))
			.build();

		HttpResponse<Void> send = client.send(rq, HttpResponse.BodyHandlers.discarding());
		assertEquals(413, send.statusCode());
	}

	@Test
	public void missingParams() throws IOException, InterruptedException {
		HttpRequest rq = builder
			.POST(HttpRequest.BodyPublishers.ofString("progress=5800&ttl=1d"))
			.build();

		HttpResponse<Void> send = client.send(rq, HttpResponse.BodyHandlers.discarding());
		assertEquals(400, send.statusCode());
	}

	@Test
	public void badTtl() throws IOException, InterruptedException {
		HttpRequest rq = builder
			.POST(HttpRequest.BodyPublishers.ofString("progress=5800&ttl=90000y"))
			.build();

		HttpResponse<Void> send = client.send(rq, HttpResponse.BodyHandlers.discarding());
		assertEquals(400, send.statusCode());
	}

	@Disabled
	public void maxSubscribers() throws IOException, InterruptedException {
		HttpRequest.Builder spamBuilder = builder
			.GET();

		AtomicInteger counter = new AtomicInteger();
		int status;
		do {
			HttpRequest rq = spamBuilder
				.uri(URI.create("http://localhost:58780/progress/testing?s=" + counter.incrementAndGet()))
				.GET().build();
			HttpResponse<Void> subResponse = client.send(rq, HttpResponse.BodyHandlers.discarding());
			status = subResponse.statusCode();
		} while (status != 503);

		assertEquals(1025, counter.get());
	}

	@Test
	public void serveStaticResources() throws IOException, InterruptedException {
		HttpRequest rq = builder
			.uri(URI.create("http://localhost:58780/index.html"))
			.GET().build();

		HttpResponse<String> index = client.send(rq, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, index.statusCode());

		rq = builder
			.uri(URI.create("http://localhost:58780/"))
			.GET().build();

		HttpResponse<String> indexRoot = client.send(rq, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, indexRoot.statusCode());

		assertEquals(index.body(), indexRoot.body());
	}
}
