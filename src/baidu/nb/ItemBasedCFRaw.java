package baidu.nb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import baidu.entity.Record;
import baidu.zjl.simmetrics.CosineSimilarity;

public class ItemBasedCFRaw {

	private int maxK;
	private HashMap<Integer, HashMap<Integer, Float>> itemRates = new HashMap<Integer, HashMap<Integer, Float>>();
	private HashMap<Integer, HashMap<Integer, Float>> userRates = new HashMap<Integer, HashMap<Integer, Float>>();
	private HashMap<Integer, TreeSet<ItemSim>> itemDists = new HashMap<Integer, TreeSet<ItemSim>>();
	private HashMap<Integer, Float> userAvg = new HashMap<Integer, Float>();
	private List<Record> predict = new ArrayList<Record>();
	private static final Logger LOG = Logger.getLogger(ItemBasedCFRaw.class);

	public static void main(String[] args) {
		ItemBasedCFRaw itcf = new ItemBasedCFRaw(400);
		// String trainf = "resource/zjl/mytrain", predictf =
		// "resource/zjl/mypredict";
		String trainf = "resource/processedData/trainingSet", predictf = "resource/processedData/predict";
		itcf.train(trainf);
		itcf.initPredict(predictf, false);
		int kset[] = { 4, 10, 15, 20, 25, 30, 35, 50, 74, 100, 130 };
		for (int k : kset) {
			LOG.info("------------------k:" + k);
			itcf.predict(k);
			itcf.outputPredict("resource/zjl/ItemBased-Cosine-" + k);
		}
	}

	public void train(String trainf) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(trainf));

			String line = null;
			String fields[];
			int userid, itemid;
			float rate;

			while ((line = br.readLine()) != null) {
				fields = line.split("\t");
				if (fields.length == 3) {
					userid = Integer.parseInt(fields[0]);
					itemid = Integer.parseInt(fields[1]);
					rate = Float.parseFloat(fields[2]);

					if (!itemRates.containsKey(itemid))
						itemRates.put(itemid, new HashMap<Integer, Float>());
					itemRates.get(itemid).put(userid, rate);

					if (!userRates.containsKey(userid))
						userRates.put(userid, new HashMap<Integer, Float>());
					userRates.get(userid).put(itemid, rate);

					if (userAvg.containsKey(userid)) {
						rate += userAvg.get(userid);
					}
					userAvg.put(userid, rate);

				} else
					LOG.warn("bad data:" + fields);
			}
			LOG.info("get total item:" + itemRates.keySet().size() + ", user:"
					+ userRates.keySet().size());

			LOG.info("calc user average rates");
			for (Integer uid : userAvg.keySet()) {
				userAvg.put(uid, userAvg.get(uid) / userRates.get(uid).size());
			}

			LOG.info("normalize user rates");
			for (Integer uid : userRates.keySet()) {
				float avg = userAvg.get(uid);
				HashMap<Integer, Float> itemMap = userRates.get(uid);
				for (Integer iid : itemMap.keySet()) {
					itemMap.put(iid, itemMap.get(iid) - avg);
				}
			}
			for (HashMap<Integer, Float> irates : itemRates.values()) {
				for (Integer uid : irates.keySet()) {
					irates.put(uid, irates.get(uid) - userAvg.get(uid));
				}
			}

			LOG.info("calculate item similarities");
			Set<Integer> visitedItems = new HashSet<Integer>();
			for (Integer itema : itemRates.keySet()) {
				visitedItems.add(itema);
				for (Integer itemb : itemRates.keySet()) {
					if (visitedItems.contains(itemb))
						continue;
					float sim = CosineSimilarity.calcSim(itemRates.get(itema),
							itemRates.get(itemb));
					if (!itemDists.containsKey(itema))
						itemDists
								.put(itema, new TreeSet<ItemSim>(itemRateComp));
					if (!itemDists.containsKey(itemb))
						itemDists
								.put(itemb, new TreeSet<ItemSim>(itemRateComp));
					TreeSet<ItemSim> itemAset = itemDists.get(itema);
					itemAset.add(new ItemSim(itemb, sim));
					if (itemAset.size() > maxK)
						itemAset.pollLast();
					TreeSet<ItemSim> itemBset = itemDists.get(itemb);
					itemBset.add(new ItemSim(itema, sim));
					if (itemBset.size() > maxK)
						itemBset.pollLast();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(e.getMessage());
		}
	}

	public void predict(int k) {

		for (Record record : predict) {
			int userid = record.getUserId();
			int itemid = record.getMovieId();
			HashMap<Integer, Float> userates = userRates.get(userid);
			TreeSet<ItemSim> neighbors = itemDists.get(itemid);
			int count = 0;
			float rateSum = 0f, simSum = 0f;
			for (ItemSim nei : neighbors) {
				// -----------
				// 使用所有近邻
				if (userates.containsKey(nei.itemId)) {
					count++;
					rateSum += userates.get(nei.itemId) * nei.sim;
					simSum += Math.abs(nei.sim);
					if (count >= k)
						break;
				}
				// ----------------
				// use 相似度>0的近邻
				// if (userates.containsKey(nei.itemId) && nei.sim > 0) {
				// count++;
				// rateSum += userates.get(nei.itemId) * nei.sim;
				// simSum += nei.sim;
				// if (count >= k)
				// break;
				// }
			}
			float rate = 0f;
			if (count > 0 && simSum != 0) {
				rate = rateSum / simSum;
			} else {
				rate = 0f;
			}
			rate += userAvg.get(userid);
			record.setScore(rate);
		}
	}

	private void outputPredict(String resultf) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(resultf));
			for (Record r : predict) {
				pw.println(r.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pw != null)
				pw.close();
		}
	}

	private void initPredict(String predictf, boolean withRate) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(predictf));
			String item = null;
			String fields[];
			int userid, itemid;
			float rate;
			while ((item = br.readLine()) != null) {
				fields = item.split("\t");
				userid = Integer.parseInt(fields[0]);
				itemid = Integer.parseInt(fields[1]);
				if (withRate) {
					rate = Float.parseFloat(fields[2]);
				} else
					rate = -1;
				predict.add(new Record(userid, itemid, rate));
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.warn(e.getMessage());
		}

	}

	public final class ItemSim {
		int itemId;
		float sim;

		public ItemSim(Integer itemId, float sim) {
			this.itemId = itemId;
			this.sim = sim;
		}
	}

	public final Comparator<ItemSim> itemRateComp = new Comparator<ItemSim>() {
		@Override
		public int compare(ItemSim itema, ItemSim itemb) {
			float c = itema.sim - itemb.sim;
			if (c > 0)
				return -1;
			else
				return c == 0 ? 0 : 1;

		}
	};

	ItemBasedCFRaw(int maxK) {
		this.maxK = maxK;
	}
}
