package net.shrimpworks.proggers.entity;

import java.beans.ConstructorProperties;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.TreeMap;

public class Progress {

	public final String id;
	public final String name;
	public final String group;
	public final double progress;
	public final double max;
	public final String color;
	public final Duration ttl;
	public final ZonedDateTime created;
	public final ZonedDateTime updated;
	public final TreeMap<Double, ZonedDateTime> history;

	@ConstructorProperties({ "id", "name", "group", "progress", "max", "color", "ttl", "created", "updated", "history" })
	public Progress(String id, String name, String group, double progress, double max, String color, Duration ttl,
					ZonedDateTime created, ZonedDateTime updated, TreeMap<Double, ZonedDateTime> history) {
		this.id = id;
		this.name = name;
		this.group = group;
		this.progress = progress;
		this.max = max;
		this.color = color == null ? "00bb00" : color;
		this.ttl = ttl == null ? Duration.ofDays(1) : ttl;
		this.created = created == null ? ZonedDateTime.now() : created;
		this.updated = updated == null ? ZonedDateTime.now() : updated;
		this.history = history == null ? new TreeMap<>() : history;
	}

	public Duration getEta() {
		if (history.size() < 2) return Duration.ZERO;

		double pDelta = (history.lastKey() - history.firstKey()) / (history.size() - 1);
		double pRemain = max - progress;
		double pSteps = pRemain / pDelta;
		Duration tDelta = Duration.between(history.firstEntry().getValue(), history.lastEntry().getValue()).dividedBy(history.size() - 1);
		return tDelta.multipliedBy((long)(Math.ceil(pSteps)));
	}

	public double getPercent() {
		if (max <= 0) return 0;
		return progress / max * 100;
	}
}
