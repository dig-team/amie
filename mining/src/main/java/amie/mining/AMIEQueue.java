package amie.mining;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import amie.rules.Rule;

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
	
	private final Condition empty = lock.newCondition(); 
	
	private LinkedHashSet<Rule> current;
	
	private LinkedHashSet<Rule> next;
	
	private int generation;
	
	private int maxThreads;
	
	private int waitingThreads = 0;
	
	public AMIEQueue(Collection<Rule> seeds, int maxThreads) {
		this.generation = 1;
		this.maxThreads = maxThreads; 
		this.waitingThreads = 0;
		this.current = new LinkedHashSet<>();
		for (Rule seed : seeds) {
			seed.setGeneration(generation);
			this.current.add(seed);
		}
		this.generation++;
		this.next = new LinkedHashSet<>();
	}
	
	/**
	 * Adds an item to the queue.
	 * @param o
	 */
	public void queue(Rule o) {
		lock.lock();
		o.setGeneration(generation);
		next.add(o);
		lock.unlock();		
	}
	
	/**
	 * Adds a collection of items to the queue.
	 * @param rules
	 */
	public void queueAll(Collection<Rule> rules) {
		lock.lock();
		for (Rule r : rules) {
			r.setGeneration(generation);
			next.add(r);			
		}
		lock.unlock();					
	}
	
	/**
	 * Retrieves and removes the oldest item that was added to the queue.
	 * @return An object or null if the queue is empty.
	 * @throws InterruptedException
	 */
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
	
	/**
	 * Retrieves and removes an item from the current queue.
	 * @return
	 */
	private Rule poll() {
    	Iterator<Rule> iterator = current.iterator();
        Rule nextItem = iterator.next();
        iterator.remove();
        return nextItem;		
	}


	private void nextGeneration() {
		generation++;
		current = next;
		next = new LinkedHashSet<>();
	}
	
	public boolean isEmpty() {
		return current.isEmpty() && next.isEmpty();
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
