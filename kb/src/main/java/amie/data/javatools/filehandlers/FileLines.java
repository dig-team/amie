package amie.data.javatools.filehandlers;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;


import amie.data.javatools.administrative.Announce;
import amie.data.javatools.datatypes.PeekIterator;

/**
 * This class is part of the Java Tools (see
 * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
 * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
 * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
 * 
 * The class provides an iterator over the lines in a file<BR>
 * Example:
 * 
 * <PRE>
 * for (String s : new FileLines(&quot;c:\\autoexec.bat&quot;)) {
 * 	System.out.println(s);
 * }
 * </PRE>
 * 
 * If desired, the iterator can make a nice progress bar by calling
 * Announce.progressStart/At/Done automatically in the right order. If there are
 * no more lines, the file is closed. If you do not use all lines of the
 * iterator, close the iterator manually.
 */
public class FileLines extends PeekIterator<String> implements
		Iterable<String>, Iterator<String>, Closeable {
	/** number of chars for announce (or -1) */
	protected long announceChars = -1;
	/** Containes the Reader */
	protected BufferedReader br;

	/** Constructs FileLines from a file */
	public FileLines(File f) throws IOException {
		this(f, null);
	}

	/**
	 * Constructs FileLines from a file with an encoding, shows progress bar
	 * (main constructor 1)
	 */
	public FileLines(File f, String encoding, String announceMsg)
			throws IOException {
		if (announceMsg != null) {
			Announce.progressStart(announceMsg, f.length());
			announceChars = 0;
		}
		br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
				encoding));
	}

	/**
	 * Constructs FileLines from a file, shows progress bar (main constructor 2)
	 */
	public FileLines(File f, String announceMsg) throws IOException {
		if (announceMsg != null) {
			Announce.progressStart(announceMsg, f.length());
			announceChars = 0;
		}
		br = new BufferedReader(new FileReader(f));
	}

	/** Unsupported, throws an UnsupportedOperationException */
	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(
				"FileLines does not support \"remove\"");
	}

	/**
	 * Returns next line. In case of an IOException, the exception is wrapped in
	 * a RuntimeException
	 */
	public String internalNext() {
		String next;
		try {
			next = br.readLine();
			if (announceChars != -1 && next != null)
				Announce.progressAt(announceChars += next.length());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return (next);
	}

	/** Returns a simple identifier */
	public String toString() {
		return ("FileLines of " + br);
	}

	/** Returns this */
	public Iterator<String> iterator() {
		return this;
	}

	/** Closes the reader */
	public void close() {
		try {
			br.close();
		} catch (IOException e) {
		}
		if (announceChars != -1)
			Announce.progressDone();
		announceChars = -1;
	}

	/** Closes the reader */
	public void finalize() {
		close();
	}

}
