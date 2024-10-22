package amie.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Three KGs with a common dictionary -- mostly for ML purposes
 */
public class Dataset {
	public AbstractKB training;
	public Map<Integer, List<int[]>> testing;

	/**
	 * It constructs a dataset from a given path. It assumes
	 * the path contains at least files train.tsv and test.tsv, and optionally
	 * valid.tsv
	 * 
	 * @param path
	 * @throws IOException
	 */
	public Dataset(String path) throws IOException {
		String basePath = path;
		if (!path.endsWith("/"))
			basePath = path + "/";

		String train = basePath + "train.tsv";
		String valid = basePath + "valid.tsv";
		KB trainingKB = new KB();
		trainingKB.load(new File[] { new File(train), new File(valid) });
		this.training = trainingKB;

		String test = basePath + "test.tsv";
		this.testing = new LinkedHashMap<>();
		for (int r : trainingKB.getRelations())
			this.testing.put(r, new ArrayList<>());
		this.loadTestTriples(new File(test));
	}

	private void loadTestTriples(File testFile) throws IOException {
		Files.lines(testFile.toPath())
				.map(line -> line.split("\t"))
				.map(e -> this.mapStrings(e))
				.forEach(e -> this.testing.get(e[1]).add(e));
	}

	private int[] mapStrings(String[] strings) {
		int[] result = new int[strings.length];
		for (int i = 0; i < strings.length; ++i) {
			result[i] = this.training.map(strings[i]);
		}

		return result;
	}
}
