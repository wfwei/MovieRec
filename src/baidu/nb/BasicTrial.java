package baidu.nb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;

import baidu.entity.Record;

public class BasicTrial extends AbstractMethod {

	public static void main(String[] args) {
		BasicTrial itcf = new BasicTrial();
		String trainf = "resource/processedData/trainingSet", predictf = "resource/processedData/predict";
		itcf.train(trainf);
		itcf.initPredict(predictf, false);

		itcf.predictUseRandom();
		itcf.outputPredict("resource/zjl/Random-predict");
		itcf.predictUseAllAVg();
		itcf.outputPredict("resource/zjl/Overall-Average-predict");
		itcf.predictUseUserAVg();
		itcf.outputPredict("resource/zjl/User-Average-predict");
		itcf.predictUseItemAVg();
		itcf.outputPredict("resource/zjl/Item-Average-predict");

	}

	private RateInfo all = new RateInfo(0, 0);
	private HashMap<Integer, RateInfo> item = new HashMap<Integer, RateInfo>();
	private HashMap<Integer, RateInfo> user = new HashMap<Integer, RateInfo>();
	private List<Record> predict = new ArrayList<Record>();
	private static final Logger LOG = Logger.getLogger(BasicTrial.class);

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

					all.addRate(rate);
					if (!item.containsKey(itemid))
						item.put(itemid, new RateInfo(rate, 1));
					item.get(itemid).addRate(rate);

					if (!user.containsKey(userid))
						user.put(userid, new RateInfo(rate, 1));
					user.get(userid).addRate(rate);

				} else
					LOG.warn("bad data:" + fields);
			}

			all.calcAvg();
			for (RateInfo rc : item.values())
				rc.calcAvg();
			for (RateInfo rc : user.values())
				rc.calcAvg();

		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
	}

	public void predictUseRandom() {
		LOG.info("Using random rates to predict");
		for (Record record : predict) {
			record.setScore(Math.random() * 5);
		}
	}

	public void predictUseAllAVg() {
		LOG.info("Using over all average to predict");
		for (Record record : predict) {
			record.setScore(all.getAvg());
		}
	}

	public void predictUseUserAVg() {

		LOG.info("Using user average to predict");
		for (Record record : predict) {
			int userid = record.getUserId();
			record.setScore(user.get(userid).getAvg());

		}
	}

	public void predictUseItemAVg() {

		LOG.info("Using item average to predict");
		for (Record record : predict) {
			int itemid = record.getMovieId();
			record.setScore(item.get(itemid).getAvg());

		}
	}

	@Override
	protected double predict(int userid, int itemid) {
		throw new RuntimeException("Not implemeted yet...");
	}

	@Override
	protected void initParas() {
		throw new RuntimeException("Not implemeted yet...");
	}

	@Override
	public String toString() {
		throw new RuntimeException("Not implemeted yet...");
	}

	@Override
	protected double calcCost() {
		throw new RuntimeException("Not implemeted yet...");
	}
}
