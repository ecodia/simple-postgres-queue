package de.ecodia.simplequeue.queue;

import javax.sql.DataSource;

import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.ecodia.simplequeue.Queue;
import de.ecodia.simplequeue.Utils;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class QueueTest {

	private DataSource ds;

	private static final String queueName = "testqueue";
	public QueueTest() throws IOException {
		EmbeddedPostgres pg = EmbeddedPostgres.start();
		this.ds = pg.getPostgresDatabase();
	}

	@Test
	public void publish() {
		var queue = new Queue(this.ds, "testhost", true, "mytest1");
		final int[] count = new int[1];
		count[0] = 0;
		queue.subscribe(queueName + "pub", message -> {
			assertEquals("{ 'foo': 'bar'}", message.getMessage());
			count[0] = 1;
			return "";
		});
		queue.publish(queueName + "pub", "{ 'foo': 'bar'}");
		Utils.sleep(200);
		assertEquals(1, count[0]);
	}

	@Test
	public void concurrent() {
		var queue = new Queue(this.ds, "testhost", true, "mytest1");
		final int[] countA = new int[1];
		countA[0] = 0;
		final int[] countB = new int[1];
		countB[0] = 0;
		final int[] countC = new int[1];
		countC[0] = 0;

		final LinkedList<String> all = new LinkedList<>();
		final LinkedList<String> errors = new LinkedList<>();

		queue.subscribe(queueName, message -> {
			countA[0]++;
			if(all.contains(message.getMessage())) {
				var msg = "Message duplicate found: " + message.getMessage();
				System.out.println(msg);
				errors.add(msg);
			}
			all.add(message.getMessage());
			return "";
		});
		queue.subscribe(queueName, message -> {
			countB[0]++;
			if(all.contains(message.getMessage())) {
				var msg = "Message duplicate found: " + message.getMessage();
				System.out.println(msg);
				errors.add(msg);
			}
			all.add(message.getMessage());
			return "";
		});
		queue.subscribe(queueName, message -> {
			countC[0]++;
			if(all.contains(message.getMessage())) {
				var msg = "Message duplicate found: " + message.getMessage();
				System.out.println(msg);
				errors.add(msg);
			}
			all.add(message.getMessage());
			return "";
		});
		for(int i = 0; i < 1000; i++) {
			queue.publish(queueName, "msg: " + i);
		}
		Utils.sleep(5000);
		if(errors.size() > 0) {
			throw new RuntimeException("More than 0 errors: " + errors.size());
		}

		assertEquals(1000, countA[0] + countB[0] + countC[0]);
	}
}