import java.util.*;

public class Posting {
	
	private int docId;
	private ArrayList<Integer> positions;
	
	public Posting(int id, ArrayList<Integer> positions) {
		this.docId = id;
		this.positions = positions;
	}
	
	public ArrayList<Integer> getPositions() {
		return positions;
	}
	
	public int getDocId() {
		return docId;
	}

}