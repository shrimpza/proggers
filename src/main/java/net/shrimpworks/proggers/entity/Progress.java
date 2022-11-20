package net.shrimpworks.proggers.entity;

import java.beans.ConstructorProperties;
import java.time.Duration;
import java.time.ZonedDateTime;

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

	@ConstructorProperties({ "id", "name", "group", "progress", "max", "color", "ttl", "created", "updated" })
	public Progress(String id, String name, String group, double progress, double max, String color, Duration ttl,
					ZonedDateTime created, ZonedDateTime updated) {
		this.id = id;
		this.name = name;
		this.group = group;
		this.progress = progress;
		this.max = max;
		this.color = color == null ? "00bb00" : color;
		this.ttl = ttl == null ? Duration.ofDays(1) : ttl;
		this.created = created == null ? ZonedDateTime.now() : created;
		this.updated = updated == null ? ZonedDateTime.now() : updated;
	}
}
