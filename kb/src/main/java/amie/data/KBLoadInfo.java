package amie.data;

/**
 * Class to summarize KB loading information.
 * @author lgalarra
 */
public class KBLoadInfo {
	public long totalLoadTime;
	public KB kb;
	
	public KBLoadInfo(long totalLoadTime, KB kb) {
		this.totalLoadTime = totalLoadTime;
		this.kb = kb;
	}
	
	public long getTotalLoadTime() {
		return totalLoadTime;
	}

	public KB getKB() {
		return kb;
	}
}