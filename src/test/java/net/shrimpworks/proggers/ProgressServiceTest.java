package net.shrimpworks.proggers;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;

import net.shrimpworks.proggers.entity.Progress;
import net.shrimpworks.proggers.service.ProgressService;
import net.shrimpworks.proggers.service.ProgressStore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProgressServiceTest {

	@Test
	public void lifecycleTest() throws IOException {
		final ProgressService service = new ProgressService(new ProgressStore.MemoryStore());

		Progress mine = service.store("Mine", "number_one", 0, 10, "000000", Duration.ofMinutes(2));
		Progress yours = service.store("yours", "Number_1", 0, 10, "FF0000", Duration.ofMinutes(2));
		assertEquals("mine_number_one", mine.id);
		assertEquals("yours_number_1", yours.id);

		assertTrue(service.delete("mine", "Number_One"));

		assertFalse(service.delete("Yours", "number_2"));
	}

	@Test
	public void validationsTest() {
		final ProgressService service = new ProgressService(new ProgressStore.MemoryStore());

		// fail to store with a bad group, name, colour, ttl
		assertThrows(IllegalArgumentException.class,
					 () -> service.store("Not yours!", "number_one", 0, 10, "000000", Duration.ofMinutes(2)));
		assertThrows(IllegalArgumentException.class, () -> service.store("mine", "Number 1 (One)", 0, 10, "000000", Duration.ofMinutes(2)));
		assertThrows(IllegalArgumentException.class, () -> service.store("mine", "number_1", 0, 10, "red", Duration.ofMinutes(2)));
		assertThrows(IllegalArgumentException.class, () -> service.store("mine", "number_1", 0, 10, "ff0000", Duration.ofDays(69)));
	}

	@Test
	public void etaTest() {
		Progress p = new Progress(
			"123", "slow", "slow", 0.2, 0.25, null, null, ZonedDateTime.now().minusMinutes(5), ZonedDateTime.now(),
			new TreeMap<>(Map.of(
				0.1d, ZonedDateTime.now().minusMinutes(2),
				0.15d, ZonedDateTime.now().minusMinutes(1),
				0.2d, ZonedDateTime.now()
			)));

		// the ETA should be 1 minute
		assertEquals(60L, p.getEta().getSeconds());

		assertEquals(80d, p.getPercent());
	}
}
