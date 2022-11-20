package net.shrimpworks.proggers;

import java.io.IOException;
import java.time.Duration;

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
		assertThrows(IllegalArgumentException.class, () -> service.store("Not yours!", "number_one", 0, 10, "000000", Duration.ofMinutes(2)));
		assertThrows(IllegalArgumentException.class, () -> service.store("mine", "Number 1 (One)", 0, 10, "000000", Duration.ofMinutes(2)));
		assertThrows(IllegalArgumentException.class, () -> service.store("mine", "number_1", 0, 10, "red", Duration.ofMinutes(2)));
		assertThrows(IllegalArgumentException.class, () -> service.store("mine", "number_1", 0, 10, "ff0000", Duration.ofDays(69)));
	}
}
