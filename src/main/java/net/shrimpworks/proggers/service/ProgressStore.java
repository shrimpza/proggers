package net.shrimpworks.proggers.service;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.proggers.JSON;
import net.shrimpworks.proggers.entity.Progress;

public interface ProgressStore {

	public Set<Progress> group(String group);

	public Set<Progress> all();

	public Progress get(String id);

	public Progress upsert(Progress updated) throws IOException;

	public boolean remove(String id) throws IOException;

	public static class MemoryStore implements ProgressStore {

		private final Map<String, Progress> progress;

		public MemoryStore() {
			this.progress = new ConcurrentHashMap<>();
		}

		@Override
		public Set<Progress> group(String group) {
			final ZonedDateTime now = ZonedDateTime.now();
			return progress.values().stream()
						   .filter(p -> p.group.equalsIgnoreCase(group))
						   .filter(p -> p.updated.plus(p.ttl).isAfter(now))
						   .collect(Collectors.toSet());
		}

		@Override
		public Set<Progress> all() {
			final ZonedDateTime now = ZonedDateTime.now();
			return progress.values().stream()
						   .filter(p -> p.updated.plus(p.ttl).isAfter(now))
						   .collect(Collectors.toSet());
		}

		@Override
		public Progress get(String id) {
			return progress.get(id);
		}

		@Override
		public Progress upsert(Progress updated) {
			this.progress.put(updated.id, updated);
			return updated;
		}

		@Override
		public boolean remove(String id) {
			return this.progress.remove(id) != null;
		}
	}

	public static class FileStore implements ProgressStore {

		private final Path root;
		private final Map<String, ProgressItem> progress;

		public FileStore(Path root) throws IOException {
			this.root = root;
			this.progress = new ConcurrentHashMap<>();

			final ZonedDateTime now = ZonedDateTime.now();
			try (Stream<Path> paths = Files.walk(root)) {
				paths.forEach(p -> {
					if (Files.isDirectory(p)) return;

					ProgressItem item = new ProgressItem(p);

					if (item.get().updated.plus(item.get().ttl).isAfter(now)) {
						progress.put(item.id, item);
					} else {
						// item has expired, remove it
						try {
							Files.deleteIfExists(p);
						} catch (IOException e) {
							// TODO LOG
						}
					}
				});
			}
		}

		public Set<Progress> group(String group) {
			final ZonedDateTime now = ZonedDateTime.now();
			return progress.values().stream()
						   .filter(p -> p.group.equalsIgnoreCase(group))
						   .map(ProgressItem::get)
						   .filter(Objects::nonNull)
						   .filter(p -> p.updated.plus(p.ttl).isAfter(now))
						   .collect(Collectors.toSet());
		}

		public Set<Progress> all() {
			final ZonedDateTime now = ZonedDateTime.now();
			return progress.values().stream()
						   .map(ProgressItem::get)
						   .filter(Objects::nonNull)
						   .filter(p -> p.updated.plus(p.ttl).isAfter(now))
						   .collect(Collectors.toSet());
		}

		public Progress get(String id) {
			ProgressItem item = progress.get(id);
			if (item == null) return null;
			return item.get();
		}

		public Progress upsert(Progress updated) throws IOException {
			ProgressItem current = this.progress.get(updated.id);

			Path path;
			if (current != null) path = current.path;
			else path = root.resolve(updated.id);

			Files.write(path, JSON.toBytes(updated));

			ProgressItem item = new ProgressItem(path, updated);

			this.progress.put(updated.id, item);

			return updated;
		}

		@Override
		public boolean remove(String id) throws IOException {
			if (id == null) return false;

			ProgressItem current = this.progress.remove(id);

			if (current == null) return false;

			return Files.deleteIfExists(current.path);
		}

		public static class ProgressItem {

			private final Path path;
			private final String group;
			private final String id;
			private SoftReference<Progress> progress;

			public ProgressItem(Path path) {
				Progress progress = load(path);

				// at startup, we expect this to be available
				if (progress == null) throw new IllegalStateException("Failed to load progress item from path " + path);

				this.path = path;
				this.group = progress.group;
				this.id = progress.id;
				this.progress = new SoftReference<>(progress);
			}

			public ProgressItem(Path path, Progress progress) {
				this.path = path;
				this.group = progress.group;
				this.id = progress.id;
				this.progress = new SoftReference<>(progress);
			}

			private Progress load(Path path) {
				try {
					return JSON.fromFile(path, Progress.class);
				} catch (IOException e) {
					return null;
				}
			}

			public Progress get() {
				Progress maybe = progress.get();
				if (maybe != null) return maybe;

				Progress progress = load(path);
				this.progress = new SoftReference<>(progress);
				return progress;
			}
		}
	}
}