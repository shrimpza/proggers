package net.shrimpworks.proggers.www;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;

import net.shrimpworks.proggers.entity.Progress;
import net.shrimpworks.proggers.service.ProgressService;

import static net.shrimpworks.proggers.service.ProgressService.DeleteConsumer;
import static net.shrimpworks.proggers.service.ProgressService.ProgressConsumer;

public class ProgressHandler extends Handler implements ProgressConsumer, DeleteConsumer {

	private static final long MAX_BODY_SIZE = 256;
	private static final int MAX_SUBSCRIBERS = 1024;
	private static final Duration SUBSCRIBER_POLL_TIME = Duration.of(30, ChronoUnit.SECONDS);
	private static final Duration SUBSCRIBER_TTL = Duration.of(5, ChronoUnit.MINUTES);

	private final ProgressService progress;

	private final WeakHashMap<String, GroupSubscriber> subscribers;

	public ProgressHandler(ProgressService progress) {
		this.progress = progress;
		this.subscribers = new WeakHashMap<>();
		this.progress.listen((ProgressConsumer)this);
		this.progress.listen((DeleteConsumer)this);
	}

	@Override
	public void accept(Progress updated) {
		final ZonedDateTime deadline = ZonedDateTime.now().minus(SUBSCRIBER_TTL);
		SubscriberUpdate update = new SubscriberUpdate(updated);
		for (GroupSubscriber subscriber : this.subscribers.values()) {
			if (subscriber.accessTime.isBefore(deadline)) continue;
			if (subscriber.group.equalsIgnoreCase(updated.group)) subscriber.queue.add(update);
			if (subscriber.group.equalsIgnoreCase("all")) subscriber.queue.add(update);
		}
	}

	@Override
	public void accept(String group, String deleted) {
		SubscriberUpdate delete = new SubscriberUpdate(deleted);
		final ZonedDateTime deadline = ZonedDateTime.now().minus(SUBSCRIBER_TTL);
		for (GroupSubscriber subscriber : this.subscribers.values()) {
			if (subscriber.accessTime.isBefore(deadline)) continue;
			if (subscriber.group.equalsIgnoreCase(group)) subscriber.queue.add(delete);
			if (subscriber.group.equalsIgnoreCase("all")) subscriber.queue.add(delete);
		}
	}

	@Override
	public void handle(HttpExchange exchange) {
		try {
			switch (exchange.getRequestMethod().toUpperCase()) {
				case "POST":
					handlePost(exchange);
					break;
				case "GET":
					handleGet(exchange);
					break;
				case "DELETE":
					handleDelete(exchange);
					break;
				default:
					respondPlain(exchange, 405, "Method not supported");
			}
		} catch (Exception e) {
			e.printStackTrace(); // TODO log
			respondPlain(exchange, 500, "Something went wrong");
		}
	}

	private void handleGet(HttpExchange exchange) throws InterruptedException {
		String[] pathParts = exchange.getRequestURI().getPath().split("/");
		if (pathParts.length < 3 || pathParts[2].isBlank()) {
			respondPlain(exchange, 404, "missing group");
			return;
		}

		final String groupName = pathParts[2];

		final Set<SubscriberUpdate> result = new HashSet<>();

		Map<String, String> params = queryParams(exchange);
		if (params.containsKey("s") && !params.get("s").isBlank()) {
			GroupSubscriber subscriber = subscribers.get(params.get("s").trim());

			// set up a new subscription
			if (subscriber == null) {
				if (subscribers.size() >= MAX_SUBSCRIBERS) {
					respondPlain(exchange, 503, "too many concurrent subscribers");
					return;
				}

				subscribers.put(params.get("s").trim(), new GroupSubscriber(groupName));

				// provide the existing elements on subscription creation
				result.addAll(progress.group(groupName).stream().map(SubscriberUpdate::new).collect(Collectors.toSet()));
			} else {
				// it's an existing subscription, wait for something to pop into its queue
				result.addAll(subscriber.poll(SUBSCRIBER_POLL_TIME));
			}
		} else {
			// no subscription, just get the current progresses for the group
			result.addAll(progress.group(groupName).stream().map(SubscriberUpdate::new).collect(Collectors.toSet()));
		}

		respondJson(exchange, 200, result);
	}

	private void handlePost(HttpExchange exchange) {
		final long contentLength = Long.parseLong(Optional.ofNullable(exchange.getRequestHeaders().getFirst("Content-Length")).orElse("0"));
		if (contentLength > MAX_BODY_SIZE) {
			respondPlain(exchange, 413, "Oversized request");
			return;
		}

		String[] pathParts = exchange.getRequestURI().getPath().split("/");
		if (pathParts.length < 3 || pathParts[2].isBlank()) {
			respondPlain(exchange, 404, "missing group");
			return;
		}
		if (pathParts.length < 4 || pathParts[3].isBlank()) {
			respondPlain(exchange, 404, "missing name");
			return;
		}

		// find initial parameters on the query string
		final Map<String, String> params = queryParams(exchange);

		// add params from the path
		params.put("group", pathParts[2]);
		params.put("name", pathParts[3]);

		// allow defining parameters in the body - these will take preference
		if (contentLength > 0) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()), (int)contentLength)) {
				String line;
				while ((line = br.readLine()) != null) {
					Matcher paramMatcher = QUERY_PARAMS.matcher(line);
					while (paramMatcher.find()) params.put(paramMatcher.group(1), paramMatcher.group(2));
				}
			} catch (Exception e) {
				respondPlain(exchange, 500, "Error processing request");
				return;
			}
		}

		try {
			upsertProgress(params);
		} catch (IllegalArgumentException e) {
			respondPlain(exchange, 400, e.getMessage());
			return;
		} catch (IllegalStateException e) {
			respondPlain(exchange, 500, e.getMessage());
			return;
		}

		respondPlain(exchange, 200, "");
	}

	private void handleDelete(HttpExchange exchange) throws IOException {
		// TODO
		String[] pathParts = exchange.getRequestURI().getPath().split("/");
		if (pathParts.length < 3 || pathParts[2].isBlank()) {
			respondPlain(exchange, 404, "missing group");
			return;
		}
		if (pathParts.length < 4 || pathParts[3].isBlank()) {
			respondPlain(exchange, 404, "missing name");
			return;
		}

		progress.delete(pathParts[2], pathParts[3]);

		respondPlain(exchange, 200, "");
	}

	private void upsertProgress(Map<String, String> params) {
		if (!params.containsKey("group")) throw new IllegalArgumentException("group is required");
		if (!params.containsKey("name")) throw new IllegalArgumentException("name is required");
		if (!params.containsKey("max")) throw new IllegalArgumentException("max is required");
		if (!params.containsKey("progress")) throw new IllegalArgumentException("progress is required");

		try {
			progress.store(
				params.get("group").trim(),
				params.get("name").trim(),
				Double.parseDouble(params.get("progress")),
				Double.parseDouble(params.get("max")),
				params.get("color"),
				params.get("ttl")
			);
		} catch (IOException e) {
			// TODO log
			throw new IllegalStateException("Failed to store requested progress");
		}
	}

	private static class GroupSubscriber {

		private final String group;
		private final BlockingQueue<SubscriberUpdate> queue;
		private ZonedDateTime accessTime;

		public GroupSubscriber(String group) {
			this.group = group;
			this.queue = new LinkedBlockingQueue<>();
			this.accessTime = ZonedDateTime.now();
		}

		public Set<SubscriberUpdate> poll(Duration pollTime) throws InterruptedException {
			final Set<SubscriberUpdate> result = new HashSet<>();
			SubscriberUpdate polled = queue.poll(pollTime.toMillis(), TimeUnit.MILLISECONDS);
			if (polled != null) {
				result.add(polled);

				// also get anything else currently in the queue
				queue.removeIf(i -> {
					result.add(i);
					return true;
				});
			}
			this.accessTime = ZonedDateTime.now();
			return result;
		}
	}

	private static class SubscriberUpdate {

		public final Progress updated;
		public final String deleted;

		public SubscriberUpdate(Progress updated) {
			this.updated = updated;
			this.deleted = null;
		}

		public SubscriberUpdate(String deleted) {
			this.deleted = deleted;
			this.updated = null;
		}
	}

}
