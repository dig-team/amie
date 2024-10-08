package amie.mining;

import static amie.data.U.increase;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * A queue implementation with barriers tailored for the AMIE mining system.
 * This implementation guarantees that rules produced in the nth-round of a
 * breath-first
 * search strategy are always dequeued and refined before any rule corresponding
 * to the (n+1)th-round.
 *
 * @author galarrag
 */
public final class AMIEQueue {
	private final Lock lock = new ReentrantLock();
	private final Lock qlock = new ReentrantLock();

	private final Condition empty = lock.newCondition();

	private Iterator<Rule> current;

	private LinkedHashSet<Rule> next;

	private int generation;

	private int maxThreads;

	private int waitingThreads = 0;

	private Int2IntMap queueCalls = new Int2IntOpenHashMap();
	private Int2IntMap queueAdded = new Int2IntOpenHashMap();

	public void printStats() {
		System.err.println("AMIE Queue statistics:");
		int gen = 1;
		while (queueCalls.containsKey(gen)) {
			System.err.println("gen: " + gen + ", calls: " + queueCalls.get(gen) + ", added: " + queueAdded.get(gen));
			gen++;
		}
	}

	public AMIEQueue(Collection<Rule> seeds, int maxThreads) {
		this.generation = 1;
		this.queueCalls.put(this.generation, 0);
		this.queueAdded.put(this.generation, 0);
		this.maxThreads = maxThreads;
		this.waitingThreads = 0;
		this.next = new LinkedHashSet<>();
		this.queueAll(seeds);
		this.nextGeneration();
		this.done = false;
	}

	/**
	 * Adds a collection of items to the queue.
	 *
	 * @param rules
	 */
	public void queueAll(Collection<Rule> rules) {
		qlock.lock();
		for (Rule r : rules) {
			increase(queueCalls, this.generation);
			r.setGeneration(generation);
			if (next.add(r)) {
				increase(queueAdded, this.generation);
			}
		}
		qlock.unlock();
	}

	private boolean done = false;

	public Rule dequeue() throws InterruptedException {
		lock.lock();
		Rule item = null;
		while (!current.hasNext() && !done) {
			++waitingThreads;
			if (waitingThreads < maxThreads) {
				empty.await();
				--waitingThreads;
			} else {
				if (next.isEmpty()) {
					done = true;
				} else {
					nextGeneration();
				}
				--waitingThreads;
				empty.signalAll();
			}
		}
		if (done) {
			item = null;
		} else {
			item = poll();
		}
		lock.unlock();
		return item;
	}

	/**
	 * Retrieves and removes an item from the current queue.
	 *
	 * @return
	 */
	private Rule poll() {
		return current.next();
	}

	private void nextGeneration() {
		generation++;
		this.queueCalls.put(this.generation, 0);
		this.queueAdded.put(this.generation, 0);
		current = next.iterator();
		next = new LinkedHashSet<>();
	}

	public void decrementMaxThreads() {
		lock.lock();
		--maxThreads;
		lock.unlock();
	}
}
