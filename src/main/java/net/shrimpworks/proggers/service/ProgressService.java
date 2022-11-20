package net.shrimpworks.proggers.service;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.shrimpworks.proggers.entity.Progress;

public class ProgressService {

	private static final String ALL = "all";
	private static final Set<String> DISALLOW_GROUPS = Set.of(ALL, "progress", "index", "static");

	private static final Pattern VALID_NAMES = Pattern.compile("[A-Za-z0-9_-]{1,32}");
	private static final Pattern COLOUR_MATCH = Pattern.compile("[A-Fa-f0-9]{6}");
	private static final String[] RANDOM_COLOURS = new String[] { "bb", "00", "bb", "00", "55", "00", "bb" };
	private static final Duration MAX_TTL = Duration.ofDays(7);
	private static final Duration DEFAULT_TTL = Duration.ofDays(1);
	private static final Pattern DURATION_STRING = Pattern.compile("(\\d+)([smhd])");

	private final ProgressStore store;

	private final Set<ProgressConsumer> updateListeners;
	private final Set<DeleteConsumer> deleteListeners;

	public ProgressService(ProgressStore store) {
		this.store = store;
		this.updateListeners = new HashSet<>();
		this.deleteListeners = new HashSet<>();
	}

	public void listen(ProgressConsumer listener) {
		this.updateListeners.add(listener);
	}

	public void listen(DeleteConsumer listener) {
		this.deleteListeners.add(listener);
	}

	public Progress store(String group, String name, double progress, double max, String color, Duration ttl) throws IOException {
		if (DISALLOW_GROUPS.contains(group.toLowerCase())) {
			throw new IllegalArgumentException("group name not allowed!");
		}

		String id = progressId(group, name);

		if (color != null && !COLOUR_MATCH.matcher(color).matches()) {
			throw new IllegalArgumentException("color must be a hex value!");
		}

		if (ttl != null && ttl.compareTo(MAX_TTL) > 0) {
			throw new IllegalArgumentException("ttl cannot be higher than " + durationToString(MAX_TTL));
		}

		Progress current = store.get(id);
		ZonedDateTime creationTime = current != null ? current.created : ZonedDateTime.now();
		color = current != null && color == null ? current.color : color;
		ttl = current != null && ttl == null ? current.ttl : ttl;

		if (color == null) color = randomColor();
		if (ttl == null) ttl = DEFAULT_TTL;

		Progress prog = new Progress(id, name, group, progress, max, color, ttl, creationTime, ZonedDateTime.now());

		// notify listeners
		updateListeners.forEach(l -> l.accept(prog));

		return store.upsert(prog);
	}

	private String randomColor() {
		return RANDOM_COLOURS[(int)(Math.random() * (float)RANDOM_COLOURS.length)]
			   + RANDOM_COLOURS[(int)(Math.random() * (float)RANDOM_COLOURS.length)]
			   + RANDOM_COLOURS[(int)(Math.random() * (float)RANDOM_COLOURS.length)];
	}

	public Progress store(String group, String name, double progress, double max, String color, String ttl) throws IOException {
		return store(group, name, progress, max, color, stringToDuration(ttl));
	}

	public boolean delete(String group, String name) throws IOException {
		String id = progressId(group, name);
		deleteListeners.forEach(l -> l.accept(group, id));
		return store.remove(id);
	}

	public Set<Progress> group(String group) {
		if (group.equalsIgnoreCase(ALL)) return store.all();
		else return store.group(group);
	}

	private String progressId(String group, String name) {
		if (!VALID_NAMES.matcher(group).matches() || !VALID_NAMES.matcher(name).matches()) {
			throw new IllegalArgumentException("group and name must match " + VALID_NAMES.pattern());
		}

		return String.format("%s_%s", group, name).toLowerCase();
	}

	private Duration stringToDuration(String str) {
		if (str == null || str.isBlank()) return null;

		Matcher matcher = DURATION_STRING.matcher(str);
		if (matcher.matches()) {
			TemporalUnit timeUnit;
			switch (matcher.group(2)) {
				case "s":
					timeUnit = ChronoUnit.SECONDS; break;
				case "h":
					timeUnit = ChronoUnit.HOURS; break;
				case "d":
					timeUnit = ChronoUnit.DAYS; break;
				default:
					timeUnit = ChronoUnit.MINUTES;
			}

			return Duration.of(Long.parseLong(matcher.group(1)), timeUnit);
		} else {
			throw new IllegalArgumentException("ttl must be in expected format, eg. 60s, 25m, 18h or 3d");
		}
	}

	private String durationToString(Duration dur) {
		if (dur == null || dur.isZero() || dur.isNegative()) return "0s";
		if (dur.toDays() > 0) return String.format("%dd", dur.toDays());
		if (dur.toHours() > 0) return String.format("%dh", dur.toHours());
		if (dur.toMinutes() > 0) return String.format("%dm", dur.toMinutes());
		return String.format("%ds", dur.toSeconds());
	}

	public interface ProgressConsumer extends Consumer<Progress>{}
	public interface DeleteConsumer extends BiConsumer<String, String> {}
}
