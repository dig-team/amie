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
import me.tongfei.progressbar.ProgressBar;

/**
 * A queue implementation with barriers tailored for the AMIE mining system.
 * This implementation guarantees that rules produced in the nth-round of a breath-first
 * search strategy are always dequeued and refined before any rule corresponding
 * to the (n+1)th-round.
 *
 * @author galarrag
 *
 */
public final class AMIEQueue {
	private final Lock lock = new ReentrantLock();
        private final Lock qlock = new ReentrantLock();

	private final Condition empty = lock.newCondition();

	private Iterator<Rule> current;

	private LinkedHashSet<Rule> next;

	private ProgressBar progressBar;

	private boolean verbose;

	private int generation;

	private int maxThreads;

	private int waitingThreads = 0;

        private Int2IntMap queueCalls = new Int2IntOpenHashMap();
        private Int2IntMap queueAdded = new Int2IntOpenHashMap();

        public void printStats() {
            System.err.println("AMIE Queue statistics:");
            int gen = 1;
            while(queueCalls.containsKey(gen)) {
                System.err.println("gen: " + gen + ", calls: " + queueCalls.get(gen) + ", added: " + queueAdded.get(gen));
                gen++;
            }
        }

	public AMIEQueue(Collection<Rule> seeds, int maxThreads, boolean verbose) {
		this.generation = 1;
                this.queueCalls.put(this.generation, 0);
                this.queueAdded.put(this.generation, 0);
		this.maxThreads = maxThreads;
		this.waitingThreads = 0;
		this.next = new LinkedHashSet<>();
		this.progressBar = null;
		this.verbose = verbose;
		this.queueAll(seeds);
		this.nextGeneration();
                this.done = false;
	}

	/**
	 * Adds an item to the queue.
	 * @param o
	 */
	public void queue(Rule o) {
		qlock.lock();
                increase(queueCalls, this.generation);
		o.setGeneration(generation);
		if (next.add(o)) {
                    increase(queueAdded, this.generation);
                }
		qlock.unlock();
	}

	/**
	 * Adds a collection of items to the queue.
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

	/**
	 * Retrieves and removes the oldest item that was added to the queue.
	 * @return An object or null if the queue is empty.
	 * @throws InterruptedException
	 */
	/*
        // Warning: This version is not protected against spurious wakeup and
        // will kill thread if a new generation is too small (even if the next
        // may be larger)
        public Rule dequeue() throws InterruptedException {
		lock.lock();
		Rule item = null;
	    if (current.isEmpty()) {
    		++waitingThreads;
	    	if (waitingThreads < maxThreads) {
	    		empty.await();
	    		--waitingThreads;
	    	} else {
	    		nextGeneration();
	    		--waitingThreads;
		    	empty.signalAll();
	    	}

	    	if (current.isEmpty()) {
	    		item = null;
	    	} else {
	    		item = poll();
	    	}
        } else {
        	item = poll();
        }
		lock.unlock();
	    return item;
	}
        */

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
                        if (this.verbose) {
                            this.progressBar.close();
                        }
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
	 * @return
	 */
	private Rule poll() {
		if (this.verbose) {
			this.progressBar.step();
		}
		return current.next();
	}


	private void nextGeneration() {
		if (this.progressBar != null) {
			this.progressBar.close();
		}

		if (this.verbose) {
			// Heuristic for calculating updateIntervalMillis considering the queue's size and the number of consumers.
			this.progressBar = new ProgressBar("Generation " + generation + ":", next.size(), Math.min(1000, next.size() * 1000 / 1000 / maxThreads));
		}

		generation++;
		this.queueCalls.put(this.generation, 0);
		this.queueAdded.put(this.generation, 0);

		current = next.iterator();
		next = new LinkedHashSet<>();
	}

	public boolean isEmpty() {
		return !current.hasNext() && next.isEmpty();
	}

	public int getGeneration() {
		return generation;
	}

	public void decrementMaxThreads() {
		lock.lock();
		--maxThreads;
		lock.unlock();
	}
}
