package baidu.zjl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import baidu.entity.Record;

public class DataUtil {

	public static final String USER_MAP = "resource/processedData/User2ID";
	public static final String MOVIE_MAP = "resource/processedData/Movie2ID";
	private static final DataUtil du = new DataUtil();
	private Map<Integer, String> user2Id;
	private Map<Integer, String> movie2Id;

	public static void main(String[] args) {
		DataUtil du = DataUtil.getInstance();
		// du.shuffleData(5, "resource/processedData/trainingSet",
		// "resource/processedData/");
		du.changeToUpLoadFile("resource/zjl/BaseLine-iter100-alpha0.05",
				"resource/zjl/BaseLine-iter100-alpha0.05-upload");
	}

	private DataUtil() {
	}

	public static DataUtil getInstance() {
		return du;
	}

	public Map<Integer, String> getUserRef() {
		if (user2Id == null) {
			user2Id = getRefFromFile(USER_MAP);
		}
		return user2Id;
	}

	public Map<Integer, String> getMovieRef() {
		if (movie2Id == null) {
			movie2Id = getRefFromFile(MOVIE_MAP);
		}
		return movie2Id;
	}

	private Map<Integer, String> getRefFromFile(String fname) {
		Map<Integer, String> re = new HashMap<Integer, String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(fname));
			String str;
			String[] fields;
			while ((str = br.readLine()) != null) {
				fields = str.split("\t");
				if (fields.length >= 2) {
					re.put(Integer.parseInt(fields[1]), fields[0]);
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return re;
	}

	public List<Record> readRecordsFromFile(String fname) {
		List<Record> rds = new ArrayList<Record>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(fname));
			String str;
			String[] fields;
			while ((str = br.readLine()) != null) {
				fields = str.split("\t");
				if (fields.length == 3) {
					rds.add(new Record(Integer.parseInt(fields[0]), Integer
							.parseInt(fields[1]), Double.parseDouble(fields[2])));
				} else {
					if (fields.length == 2) {
						rds.add(new Record(Integer.parseInt(fields[0]), Integer
								.parseInt(fields[1]), 0));
					}
				}
			}
			br.close();
			return rds;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	private void writeRecordsToFile(List<Record> records, String fileName,
			boolean append) {

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName,
					append));
			for (Record re : records) {
				bw.write(new StringBuilder().append(re.getUserId())
						.append("\t").append(re.getMovieId()).append("\t")
						.append(re.getScore()).append("\n").toString());
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void shuffleData(int n, String srcFile, String desDir) {
		List<Record> records = readRecordsFromFile(srcFile);
		HashMap<Integer, ArrayList<Record>> userates = new HashMap<Integer, ArrayList<Record>>();
		for (Record rec : records) {
			int userid = rec.getUserId();
			if (!userates.containsKey(userid))
				userates.put(userid, new ArrayList<Record>());
			userates.get(userid).add(rec);
		}
		records.clear();
		List<Record> test = new ArrayList<Record>();
		for (int i = 0; i < n; i++) {
			int trainCount = 0, testCount = 0;
			for (List<Record> rates : userates.values()) {
				int testSize = (rates.size() - 10) / 10;
				while (testSize > 0) {
					int idx = (int) (Math.random() * rates.size());
					test.add(rates.remove(idx));
					testSize--;
				}

				writeRecordsToFile(rates, desDir + "/training-" + i, true);
				writeRecordsToFile(test, desDir + "/testing-" + i, true);
				trainCount += rates.size();
				testCount += test.size();

				rates.addAll(test);
				test.clear();
			}
			System.out.println(i + "\ttraining:" + trainCount + "\ttesting:"
					+ testCount);
		}

	}

	public void changeToUpLoadFile(String origin, String upload) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(upload));
			List<Record> records = readRecordsFromFile(origin);
			Map<Integer, String> user = getUserRef();
			Map<Integer, String> movie = getMovieRef();
			DecimalFormat df = new DecimalFormat("0.0000");
			for (Record r : records) {
				bw.write(new StringBuilder().append(user.get(r.getUserId()))
						.append("\t").append(movie.get(r.getMovieId()))
						.append("\t").append(df.format(r.getScore()))
						.append("\r\r\n").toString());
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
