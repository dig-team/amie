package amie.data.javatools.administrative;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Vector;

/**
 * This class is part of the Java Tools (see
 * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
 * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
 * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
 * 
 * 
 * 
 * 
 * 
 * This class can make progress announcements. The announcements are handled by
 * an object, but static methods exist to simplify the calls.<BR>
 * Example:
 * 
 * <PRE>
 *  Announce.doing("Testing 1");
 *  Announce.doing("Testing 2");
 *  Announce.message("Now testing", 3);
 *  Announce.warning(1,2,3);
 *  Announce.debug(1,2,3);
 *  Announce.doing("Testing 3a");
 *  Announce.doneDoing("Testing 3b");
 *  Announce.done();
 *  Announce.progressStart("Testing 3c",5); // 5 steps
 *  D.waitMS(1000);
 *  Announce.progressAt(1); // We're at 1 (of 5)
 *  D.waitMS(3000);
 *  Announce.progressAt(4); // We're at 4 (of 5)
 *  D.waitMS(1000);
 *  Announce.progressDone();
 *  Announce.done();
 *  Announce.done();
 *  Announce.done(); // This is one too much, but it works nevertheless
 *  -->
 *  Testing 1...
 *  Testing 2...
 *  Now testing 3
 *  Warning:1 2 3
 *  Announce.main(243): 1 2 3
 *  Testing 3a... done
 *  Testing 3b... done
 *  Testing 3c...........(4.00 s to go)................................ done (5.00 s)
 *  done
 *  done
 * </PRE>
 * 
 * The progress bar always walks to MAXDOTS dots. The data is written to
 * Announce.out (by default System.err). There are different levels of
 * announcements that can be switched on and off.
 */
public class Announce {

	/** Log level */
	public enum Level {
		MUTE, ERROR, WARNING, STATE, MESSAGES, DEBUG
	};

	/** Current log level */
	protected static Level level = Level.MESSAGES;

	/** Maximal number of dots */
	public static int MAXDOTS = 40;

	/** Where to write to (default: System.err) */
	protected static Writer out = new BufferedWriter(new OutputStreamWriter(
			System.err));

	/** Indentation level */
	protected static int doingLevel = 0;

	/** Are we at the beginning of a line? */
	protected static boolean cursorAtPos1 = true;

	/** Progress level */
	protected static int progressLevel = -1;

	/** Memorizes the maximal value for progressAt(...) */
	protected static double[] progressEnd = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	/** Memorizes the number of printed dots */
	protected static int[] progressDots = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	/** Memorizes a short identification string for a progress level */
	protected static String[] progressID = new String[10];

	/** Memorizes the process start time */
	protected static long[] progressStart = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	/** Internal counter for progresses */
	protected static double[] progressCounter = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	/** Did we print the estimated time? */
	protected static boolean[] printedEstimatedTime = new boolean[10];

	/** last time we printed estimated time */
	protected static long[] lastEstimation = new long[10];

	/** Memorizes the timer */
	protected static long timer;

	/**
	 * Memorizes a set of named timers that each can accumulate several time
	 * intervals
	 */
	protected static Vector<Long> timerStarts = new Vector<Long>();
	protected static Vector<Long> timerTimes = new Vector<Long>();

	/** TRUE if debugging is on */
	protected static boolean debug;

	/** Starts the timer */
	public static void startTimer() {
		timer = System.currentTimeMillis();
	}

	/** Retrieves the time */
	public static long getTime() {
		return (System.currentTimeMillis() - timer);
	}

	/** Initiates a numbered timer */
	public static int initTimer() {
		timerStarts.add(0l);
		timerTimes.add(0l);
		return timerStarts.size() - 1;
	}

	/**
	 * Starts a named timer
	 * 
	 * @note The numerical identifier for the timer has to be a number generated
	 *       by calling initTimer beforehand
	 */
	public static void startTimer(int number) {
		if (timerStarts.size() <= number)
			return;
		timerStarts.set(number, System.currentTimeMillis());
	}

	/** Stops a named timer */
	public static void stopTimer(int number) {
		if (timerTimes.size() <= number)
			return;
		timerTimes
				.set(number,
						timerTimes.get(number)
								+ (System.currentTimeMillis() - timerStarts
										.get(number)));
	}

	/** Retrieves the accumulated time the named timer has run for */
	public static Long getTime(int number) {
		return timerTimes.get(number);
	}

	/**
	 * Prints the time of a particular timer given by its timer number, may be
	 * given a name for printout
	 */
	public static void printTime(String name, int timerNumber) {
		if (name != null)
			message("Time", name, ":",
					String.format("%d s", getTime(timerNumber) / 1000));
		else
			message("Timer", timerNumber, ":",
					String.format("%d s", getTime(timerNumber) / 1000));
	}

	/** Blanks */
	public static final String blanks = "                                                                  ";

	/** Returns blanks */
	public static String blanks(int n) {
		if (n <= 0)
			return ("");
		if (n >= blanks.length())
			return (blanks);
		return (blanks.substring(0, n));
	}

	/** Returns blanks */
	protected static String blanks() {
		return (blanks(doingLevel * 2));
	}

	/** Internal printer */
	protected static void print(Object... o) {
		try {
			if (cursorAtPos1)
				out.write(blanks());
			out.write(D.toString(o).replace("\n", "\n" + blanks()));
			out.flush();
		} catch (IOException e) {
		}
		cursorAtPos1 = false;
	}

	/** Internal printer for new line */
	protected static void newLine() {
		if (cursorAtPos1)
			return;
		try {
			out.write("\n");
			out.flush();
		} catch (IOException e) {
		}
		cursorAtPos1 = true;
	}

	/**
	 * Prints an (indented) message intended to provide additional information
	 * to the user at a top level
	 */
	public static void message(Object... o) {
		if (D.smaller(level, Level.MESSAGES))
			return;
		newLine();
		if (debug)
			print("[" + CallStack.toString(new CallStack().ret().top()) + "] ");
		print(o);
		newLine();
	}

	/** Prints a debug message with the class and method name preceeding */
	public static boolean debug(Object... o) {
		if (D.smaller(level, Level.DEBUG))
			return (true);
		newLine();
		print("[" + CallStack.toString(new CallStack().ret().top()) + "] ");
		print(o);
		newLine();
		return (true);
	}

	/**
	 * Prints an error message and aborts by exiting (aborts even if log level
	 * is mute)
	 */
	public static void error(Object... o) {
		if (D.smaller(level, Level.ERROR))
			System.exit(255);
		if (doingLevel > 0)
			failed();
		doingLevel=0;
		newLine();
		if (debug)
			print("[!Error: " + CallStack.toString(new CallStack().ret().top())
					+ "] ");
		else
			print("Error: ");
		print(o);
		newLine();
		//print(new CallStack().ret());
		System.exit(255);
	}


	/** Prints a warning */
	public static void warning(Object... o) {
		if (D.smaller(level, Level.WARNING))
			return;
		newLine();
		if (debug)
			print("[!Warning:  "
					+ CallStack.toString(new CallStack().ret().top()) + "] ");
		else
			print("Warning: ");
		doingLevel += 5;
		print(o);
		doingLevel -= 5;
		newLine();
	}


	protected static void writeDoing(Object... o) {
		if (D.smaller(level, Level.STATE))
			return;
		newLine();
		if (debug)
			print("[" + CallStack.toString(new CallStack().ret().ret().top())
					+ "] ");
		print(o);
		print("... ");
		doingLevel++;
	}

	/**
	 * Writes "s..." - intended for major states telling a user what the program
	 * does
	 */
	public static void doing(Object... o) {
		if (D.smaller(level, Level.STATE))
			return;
		writeDoing(o);
	}

	/** Writes "failed NEWLINE" */
	public static void failed() {
		if (doingLevel > 0) {
			doingLevel--;
			if (D.smaller(level, Level.STATE))
				return;
			print("failed");
			newLine();
		}
	}

	/** Writes "done NEWLINE" - closes a doing statement */
	public static void done() {
		if (D.smaller(level, Level.STATE))
			return;
		if (doingLevel > 0) {
			doingLevel--;
			print("done");
			newLine();
		}
	}

	/** Writes "done NEWLINE" - closes a doing statement */
	public static void done(String text) {
		if (doingLevel > 0) {
			doingLevel--;
			if (D.smaller(level, Level.STATE))
				return;
			print(text);
			newLine();
		}
	}


	/**
	 * Calls done() and doing(...) - closes a doing statement opening the next
	 * one
	 */
	public static void doneDoing(Object... s) {
		done();
		doing(s);
	}

	/** Writes s, prepares to make progress up to max */
	public static void progressStart(String s, double max) {
		progressStart(s, null, max);
	}

	/**
	 * Writes s, prepares to make progress up to max, remembers id as name for
	 * progress counter
	 */
	public static void progressStart(String s, String id, double max) {
		progressStart(s, id, max, Level.MUTE);
	}

	/**
	 * Writes s, prepares to make progress up to max remembers id as name for
	 * progress counter (and prints it if necessary) (takes only effect iff
	 * current Announce level >= the given lvl)
	 */
	public static synchronized void progressStart(String s, String id,
			double max, Level lvl) {
		if (D.smaller(level, lvl))
			return;
		if (progressLevel < 9)
			progressLevel++;
		progressID[progressLevel] = (id != null ? "[" + id + "]: " : "");
		progressEnd[progressLevel] = max;
		progressDots[progressLevel] = 0;
		progressStart[progressLevel] = System.currentTimeMillis();
		printedEstimatedTime[progressLevel] = false;
		progressCounter[progressLevel] = 0;
		if (!D.smaller(level, Level.STATE)) {
			newLine();
			if (debug)
				print("[" + CallStack.toString(new CallStack().ret().top())
						+ "] ");
			print(s, progressID[progressLevel], "...");
		}
		doingLevel++;
	}


	/**
	 * Notes that the progress is at d, prints dots if necessary, calculates and
	 * displays the estimated time after 60sec of the progress then again after
	 * every 30min
	 */
	public static void progressAt(double d) {
		if (progressLevel < 0)
			return;
		if (d > progressEnd[progressLevel])
			return;
		if (!D.smaller(level, Level.STATE)
				&& !printedEstimatedTime[progressLevel]
				&& System.currentTimeMillis() - progressStart[progressLevel] > 60000) {
			print("("
					+ progressID[progressLevel]
					+ String.format("%d s", (long) ((System.currentTimeMillis() - progressStart[progressLevel])
					* (progressEnd[progressLevel] - d) / d) / 1000)
					+ " to go)");
			printedEstimatedTime[progressLevel] = true;
			lastEstimation[progressLevel] = System.currentTimeMillis();
		}
		if (!D.smaller(level, Level.STATE)
				&& printedEstimatedTime[progressLevel]
				&& (System.currentTimeMillis() - lastEstimation[progressLevel]) > 1800000) {
			print("("
					+ progressID[progressLevel]
					+ (String.format("%d s", (long) ((System.currentTimeMillis() - progressStart[progressLevel])
							* (progressEnd[progressLevel] - d) / d) / 1000))
					+ " to go)");
			lastEstimation[progressLevel] = System.currentTimeMillis();
		}
		if (d * MAXDOTS / progressEnd[progressLevel] <= progressDots[progressLevel])
			return;

		StringBuilder b = new StringBuilder();
		while (d * MAXDOTS / progressEnd[progressLevel] > progressDots[progressLevel]) {
			progressDots[progressLevel]++;
			b.append(".");
		}
		if (!D.smaller(level, Level.STATE))
			print(b);
	}

	/** Fills missing dots and writes "done NEWLINE" */
	public static void progressDone() {
		if (progressLevel < 0)
			return;
		progressAt(progressEnd[progressLevel]);
		doingLevel--;
		if (!D.smaller(level, Level.STATE)) {
			print(" done "
					+ "("
					+ progressID[progressLevel]
					+ String.format("%d s", (System.currentTimeMillis()
							- progressStart[progressLevel]) / 1000) + ")");
			newLine();
		}
		progressLevel--;
		if (progressLevel < -1)
			progressLevel = -1;
	}

	/** Writes a help text and exits */
	public static void help(Object... o) {
		if (D.smaller(level, Level.ERROR))
			System.exit(63);
		newLine();
		for (Object s : o) {
			print(s);
			newLine();
		}
		System.exit(63);
	}

	/** Retrieves the time */
	public static void printTime() {
		message("Time:",String.format("%d s", getTime() / 1000));
	}

	/** Test routine */
	public static void main(String[] args) {
		int doingStuff = Announce.initTimer();
		int doingOtherStuff = Announce.initTimer();
		Announce.startTimer();
		Announce.doing("Testing 1");
		Announce.doing("Testing 2");
		Announce.message("Now testing", 3);
		Announce.warning(1, 2, 3);
		Announce.debug(1, 2, 3);
		Announce.doing("Testing 3a");
		Announce.doneDoing("Testing 3b");
		Announce.done();
		Announce.progressStart("Testing 3c", 5); // 5 steps
		D.waitMS(1000);
		Announce.progressAt(1); // We're at 1 (of 5)
		Announce.startTimer(doingStuff); // we are doing some special sort of
											// computation
		D.waitMS(3000);
		Announce.progressAt(4); // We're at 4 (of 5)

		D.waitMS(1000);
		Announce.stopTimer(doingStuff); // we are done with that kind of
										// computation
		Announce.progressDone();
		Announce.progressStart("Testing 3d", 5); // 5 steps
		D.waitMS(1000);
		Announce.progressAt(1); // We're at 1 (of 5)
		Announce.progressStart("Testing 4a inside 3d", 4); // 4 step sub-process
		Announce.startTimer(doingOtherStuff); // we are doing some other kind of
												// computations
		D.waitMS(1000);
		Announce.stopTimer(doingOtherStuff); // and we are done with these
												// computations
		Announce.progressAt(1); // We're at 1 (of 4)
		D.waitMS(2000);
		Announce.progressAt(3); // We're at 3 (of 4)
		Announce.startTimer(doingStuff); // now again we do the same sort of
											// computations as in the first case
		D.waitMS(1000);
		Announce.stopTimer(doingStuff); // and we are done with it again
		Announce.progressDone(); // we're done with the inner process 4a, going
									// on with the outher process
		Announce.progressAt(2); // We're at 2 (of 5) in the outher process
		Announce.progressStart("Testing 4b inside 3d", "4b", 6); // 6 step
																	// sub-process,
																	// which we
																	// give a
																	// short ID
																	// to be
																	// included
																	// in the
																	// progress
																	// messages
		D.waitMS(1000);
		Announce.progressAt(1); // We're at 1 (of 6)
		D.waitMS(3000);
		Announce.progressAt(4); // We're at 4 (of 6)
		D.waitMS(1000);
		Announce.progressDone(); // we're done with the inner process 4b, going
									// on with the outher process
		D.waitMS(1000);
		Announce.progressAt(4); // We're at 4 (of 5)
		D.waitMS(1000);
		Announce.progressDone();// we're done with the outer process 3d
		Announce.progressDone();// This is one too much, but it works
								// nevertheless
		Announce.done();
		Announce.done();
		Announce.done(); // This is one too much, but it works nevertheless
		Announce.printTime(); // Printing the overall time
		Announce.printTime("doing stuff", doingStuff); // Printing time used to
														// "do stuff"
		Announce.printTime("doing other stuff", doingOtherStuff); // Printing
																	// time used
																	// to
																	// "do other stuff"

	}
}