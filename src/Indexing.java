import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Indexing {
	
	private Map<String, ArrayList<Posting>> invertedIndex;
	private Map<Integer, String> scenes;
	private Map<Integer , String> plays;
	
	public Indexing() {
		this.invertedIndex = new HashMap<String, ArrayList<Posting>>();
		this.scenes = new HashMap<Integer , String>();
		this.plays = new HashMap<Integer, String>();
	}
	
	// Indexing Process
	public Map<String, ArrayList<Posting>> index(JSONArray docs) {
		int docid = 0;
		int position;
		for (int i = 0; i < docs.size(); i++) {
			++docid;
			JSONObject curr = (JSONObject)docs.get(i);
			String[] toks = ((String)curr.get("text")).split("\\s+");
			String sceneId = (String)curr.get("sceneId");
			String playId = (String)curr.get("playId");
			scenes.put(docid, sceneId);
			plays.put(docid, playId);
			position = 0;
			for (int j = 0; j < toks.length; j++) {
				++position;
				if (!invertedIndex.containsKey(toks[j])) {
					ArrayList<Posting> postings = new ArrayList<Posting>();
					ArrayList<Integer> positions = new ArrayList<Integer>();
					positions.add(position);
					postings.add(new Posting(docid, positions));
					invertedIndex.put(toks[j], postings);
				} else {
					ArrayList<Posting> temp = invertedIndex.get(toks[j]);
					boolean flag = false;
					for (int k = 0; k < temp.size(); k++) {
						if (temp.get(k).getDocId() == docid) {
							temp.get(k).getPositions().add(position);
							flag = true;
							break;
						}
					}
					if (!flag) {
						ArrayList<Integer> positions = new ArrayList<Integer>();
						positions.add(position);
						temp.add(new Posting(docid, positions));
					}
				}
			}
		}
		return invertedIndex;
	}
	
	// Matching windows, find phrases in a list of postings with the same docid
	public boolean matching(ArrayList<Posting> postings, int distance) {
		ArrayList<Integer> pos0 = postings.get(0).getPositions();
		for (int i = 0; i < pos0.size(); i++) {
			int prev = pos0.get(i);
			for (int j = 1; j < postings.size(); j++) {
				ArrayList<Integer> pos = postings.get(j).getPositions();
				for (int k = 0; k < pos.size(); k++) {
					int cur = pos.get(k);
					if (prev < cur && cur <= (prev + distance)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	// Method to get the a list of scenes that contains the given phrase
	public ArrayList<String> getPhraseScenes(String line, int distance) {
		ArrayList<Posting> matches = new ArrayList<Posting>();
		String[] terms = line.split(" ");
		int docid;
		for (int j = 0; j < terms.length; j++) {
			ArrayList<Posting> currPost = invertedIndex.get(terms[j]);
			for (int i = 0; i < currPost.size(); i++) {
				ArrayList<Posting> newList = new ArrayList<Posting>();
				docid = currPost.get(i).getDocId();
				newList.add(currPost.get(i));
				if (j + 1 < terms.length) {
					ArrayList<Posting> nextPost = invertedIndex.get(terms[j + 1]);
					for (int k = 0; k < nextPost.size(); k++) {
						if (nextPost.get(k).getDocId() == docid) {
							newList.add(nextPost.get(k));
							break;
						}
					}
					if (matching(newList, distance)) {
						matches.add(currPost.get(i));
					}
				}
			}
		}
		return findPhraseScenes(matches);
	}
	
	// Method to find all the plays with the matching word
	public ArrayList<String> findPlays(String target) {
		ArrayList<String> targetPlays = new ArrayList<String>();
		ArrayList<Integer> docIDs = new ArrayList<Integer>();
		if (invertedIndex.containsKey(target)) {
			ArrayList<Posting> currPost = invertedIndex.get(target);
			for (int i = 0; i < currPost.size(); i++) {
				docIDs.add(currPost.get(i).getDocId());
			}
		}
		for (int j = 0; j < docIDs.size(); j++) {
			if (plays.containsKey(docIDs.get(j))) {
				if (!targetPlays.contains(plays.get(docIDs.get(j)))) {
					targetPlays.add(plays.get(docIDs.get(j)));
				}
			}
		}
		return targetPlays;
	}
	
	// Method to find all the scenes with the matching word
	public ArrayList<String> findScenes(String target) {
		ArrayList<String> targetScenes = new ArrayList<String>();
		ArrayList<Integer> docIDs = new ArrayList<Integer>();
		if (invertedIndex.containsKey(target)) {
			ArrayList<Posting> currPost = invertedIndex.get(target);
			for (int i = 0; i < currPost.size(); i++) {
				docIDs.add(currPost.get(i).getDocId());
			}
		}
		for (int j = 0; j < docIDs.size(); j++) {
			if (scenes.containsKey(docIDs.get(j))) {
				if (!targetScenes.contains(scenes.get(docIDs.get(j)))) {
					targetScenes.add(scenes.get(docIDs.get(j)));
				}
			}
		}
		return targetScenes;
	}

	//Helper method to find all the scenes with a given phrase
	public ArrayList<String> findPhraseScenes(ArrayList<Posting> posts) {
		ArrayList<String> targetScenes = new ArrayList<String>();
		ArrayList<Integer> docIDs = new ArrayList<Integer>();
		for (int i = 0; i < posts.size(); i++) {
			docIDs.add(posts.get(i).getDocId());
		}
		for (int j = 0; j < docIDs.size(); j++) {
			if (scenes.containsKey(docIDs.get(j))) {
				if (!targetScenes.contains(scenes.get(docIDs.get(j)))) {
					targetScenes.add(scenes.get(docIDs.get(j)));
				}
			}
		}
		return targetScenes;
	}
	
	//Method to combine two given lists and return the combined list
	public ArrayList<String> combineLists(ArrayList<String> list1, ArrayList<String> list2) {
		ArrayList<String> newList = new ArrayList<String>();
		for (int i = 0; i < list1.size(); i++) {
			newList.add(list1.get(i));
		}
		for (int j = 0; j < list2.size(); j++) {
			if (!newList.contains(list2.get(j))) {
				newList.add(list2.get(j));
			}
		}
		return newList;
	}
	
	// Method to find the scenes where term1 or term2 are more frequently used than term3
	public ArrayList<String> findScenesCompare(String term1, String term2, String term3) {
		ArrayList<Integer> scenesDocIds = new ArrayList<Integer>();
		ArrayList<String> targets = new ArrayList<String>();
		ArrayList<Posting> term1Posts = invertedIndex.get(term1);
		ArrayList<Posting> term2Posts = invertedIndex.get(term2);
		ArrayList<Posting> term3Posts = invertedIndex.get(term3);
		int docId = 0;
		for (int i = 0; i < term3Posts.size(); i++) {
			docId = term3Posts.get(i).getDocId();
			for (int j = 0; j < term1Posts.size(); j++) {
				if (docId == term1Posts.get(j).getDocId() && term3Posts.get(i).getPositions().size() < term1Posts.get(j).getPositions().size()) {
					if (!scenesDocIds.contains(term1Posts.get(j).getDocId())) {
						scenesDocIds.add(term1Posts.get(j).getDocId());
					}
				}
			}
			for (int k = 0; k < term2Posts.size(); k++) {
				if (docId == term2Posts.get(k).getDocId() && term3Posts.get(i).getPositions().size() < term2Posts.get(k).getPositions().size()) {
					if (!scenesDocIds.contains(term2Posts.get(k).getDocId())) {
						scenesDocIds.add(term2Posts.get(k).getDocId());
					}
				}
			}
		}
		for (int l = 0; l < scenesDocIds.size(); l++) {
			if (scenes.containsKey(scenesDocIds.get(l))) {
				if (!targets.contains(scenes.get(scenesDocIds.get(l)))) {
					targets.add(scenes.get(scenesDocIds.get(l)));
				}
			}
		}
		return targets;
	}
	
	//Method to output a String list into a .txt file
	public void outputToTxt(String path, ArrayList<String> list) throws IOException {
		Path outputFile = Path.of(path);
		String str = "";
		for (int i = 0; i < list.size(); i++) {
			if (str.equals("")) {
				str += list.get(i);
			} else {
				str += "\n" + list.get(i);
			}
		}
		Files.writeString(outputFile, str);
	}
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
		
		// Read in json file
		JSONParser jsonparser = new JSONParser();
		Object obj = jsonparser.parse(new FileReader("D:\\UMass Amherst Fall 2021\\Compsci 446\\Project 3\\shakespeare-scenes.json"));
		JSONObject json = (JSONObject)obj;
		JSONArray jsonArr = (JSONArray)json.get("corpus");
		
		Indexing indexObj = new Indexing();
		indexObj.index(jsonArr); 
		
		//Output terms0.txt (the scene(s) where the words thee or thou are used more frequently than the word you)
		ArrayList<String> terms0 = indexObj.findScenesCompare("thee", "thou", "you");
		Collections.sort(terms0);
		indexObj.outputToTxt("D:\\UMass Amherst Fall 2021\\Compsci 446\\Project 3\\terms0.txt", terms0);
		
		//Output terms1.txt (the play(s) where the place venice, rome, or denmark are mentioned)
		ArrayList<String> terms11 = indexObj.findScenes("venice");
		ArrayList<String> terms12 = indexObj.findScenes("rome");
		ArrayList<String> terms13 = indexObj.findScenes("denmark");
		ArrayList<String> terms1 = indexObj.combineLists(indexObj.combineLists(terms11, terms12), terms13);
		Collections.sort(terms1);
		indexObj.outputToTxt("D:\\UMass Amherst Fall 2021\\Compsci 446\\Project 3\\terms1.txt", terms1);
		
		//Output terms2.txt (the play(s) where the name goneril is mentioned)
		ArrayList<String> terms2 = indexObj.findPlays("goneril");
		Collections.sort(terms2);
		indexObj.outputToTxt("D:\\UMass Amherst Fall 2021\\Compsci 446\\Project 3\\terms2.txt", terms2);
		
		//Output terms3.txt (the play(s) where the word soldier is mentioned)
		ArrayList<String> terms3 = indexObj.findPlays("soldier");
		Collections.sort(terms3);
		indexObj.outputToTxt("D:\\UMass Amherst Fall 2021\\Compsci 446\\Project 3\\terms3.txt", terms3);
		
		//Output phrase0.txt (the scene(s) where "poor yorick" is mentioned)
		ArrayList<String> phrase0 = indexObj.getPhraseScenes("poor yorick", 1);
		Collections.sort(phrase0);
		indexObj.outputToTxt("D:\\UMass Amherst Fall 2021\\Compsci 446\\Project 3\\phrase0.txt", phrase0);
		
		//Output phrase1.txt (the scene(s) where "wherefore art thou romeo" is mentioned)
		ArrayList<String> phrase1 = indexObj.getPhraseScenes("wherefore romeo", 3);
		Collections.sort(phrase1);
		indexObj.outputToTxt("D:\\UMass Amherst Fall 2021\\Compsci 446\\Project 3\\phrase1.txt", phrase1);
		
		//Output phrase2.txt (the scene(s) where "let slip" is mentioned)
		ArrayList<String> phrase2 = indexObj.getPhraseScenes("let slip", 1);
		Collections.sort(phrase2);
		indexObj.outputToTxt("D:\\UMass Amherst Fall 2021\\Compsci 446\\Project 3\\phrase2.txt", phrase2);
		
	}
}
