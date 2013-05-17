package baidu.nb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import baidu.entity.Record;
import baidu.nb.AbstractMethod.RateInfo;

/**
 * FNM(Factorized Neighborhood Model)<b>
 * 
 * 使用 mu+bu+bi+qi*(xu+yu)进行预测
 * 
 * @author WangFengwei
 */
public class FNM extends AbstractMethod {

	protected int feature;
	protected HashMap<Integer, HashMap<Integer, Double>> userRates;
	protected List<List<Double>> Q; // itemCount × feature
	protected List<List<Double>> X; // itemCount × feature
	protected List<List<Double>> Y; // itemCount × feature
	protected static final Logger LOG = Logger.getLogger(FNM.class);

	public FNM(int feature, int iterCount, double alpha, double lambda) {
		this.feature = feature;
		this.iterCount = iterCount;
		this.alpha = alpha;
		this.lambda = lambda;
	}

	public static void main(String[] args) {
		int feature = 100, iterCount = 100;
		double alpha = 0.05, lamda = 0.005;
		FNM bl = new FNM(feature, iterCount, alpha, lamda);

		String trainf = "resource/processedData/trainingSet";
		String predictf = "resource/processedData/predict";
		bl.train(trainf);
		bl.initPredict(predictf, false);
		bl.predict();
		bl.outputPredict("resource/zjl/BaseLine-iter" + iterCount + "-alpha"
				+ alpha);
	}

	@Override
	public void train(String trainf) {

		try {
			// 读取record
			readData(trainf);

			// 初始化参数
			initParas();

			// 初始化Bui
			initBui();

			// FNM训练
			trainFNM();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void trainFNM() {
		double lastRMSE = 100d;

		for (int iter = 0; iter < iterCount; iter++) {
			for (Integer userid : userRates.keySet()) {
				List<Double> pu = new ArrayList<Double>();
				List<Double> pu2 = new ArrayList<Double>();

				// Calculate pu
				double rateCoef = 0d, binCoef = 0d;
				int totRate = userRates.get(userid).size();
				for (Entry<Integer, Double> itemrate : userRates.get(userid)
						.entrySet()) {
					int itemid = itemrate.getKey();
					double rate = itemrate.getValue();
					for (int f = 0; f < feature; f++) {
						pu.set(f,
								(rate - bPredict(userid, itemid))
										* X.get(itemid).get(f));
						rateCoef += rate * rate;
						pu2.set(f, Y.get(itemid).get(f));
					}
				}
				rateCoef = Math.pow(rateCoef, -0.5d);
				binCoef = Math.pow(totRate, -0.5d);

				for (int f = 0; f < feature; f++) {
					pu.set(f, pu.get(f) * rateCoef + pu2.get(f) * binCoef);
				}

				// gradient descent
				List<Double> sum = newRandList(feature, 0, 0);
				for (Entry<Integer, Double> itemrate : userRates.get(userid)
						.entrySet()) {
					int itemid = itemrate.getKey();
					double rate = itemrate.getValue();
					double pui = predict(userid, itemid, pu);
					double Eui = rate - pui;

					for (int f = 0; f < feature; f++) {
						sum.set(f, sum.get(f) + Eui * Q.get(itemid).get(f));
					}
					
					// HERE

				}

			}
			/**
			 * for (Record rd : records) { int u = rd.getUserId() - 1; int i =
			 * rd.getMovieId() - 1;
			 * 
			 * double rui = rd.getScore(); double pui = predict(u, i); double bu
			 * = users.get(u).getAvg(); double bi = items.get(i).getAvg();
			 * 
			 * double gradientBu = lambda * bu - (rui - pui); double gradientBi
			 * = lambda * bi - (rui - pui);
			 * 
			 * users.get(u).setAvg(bu - alpha * gradientBu);
			 * items.get(i).setAvg(bi - alpha * gradientBi); } double rmse =
			 * calcRMSE(); if (rmse > 1) alpha *= 1.05; else alpha *= 0.95;
			 * LOG.info("iterater:" + iter + "\trmse:" + rmse); if (lastRMSE -
			 * rmse < 0.00001) break; else lastRMSE = rmse;
			 **/
		}
	}

	/**
	 * 使用梯度下降求解Bui
	 */
	private void initBui() {
		double lastRMSE = 100d;

		for (int iter = 0; iter < iterCount; iter++) {
			for (Record rd : records) {
				int u = rd.getUserId() - 1;
				int i = rd.getMovieId() - 1;

				double rui = rd.getScore();
				double pui = predict(u, i);
				double bu = users.get(u).getAvg();
				double bi = items.get(i).getAvg();

				double gradientBu = lambda * bu - (rui - pui);
				double gradientBi = lambda * bi - (rui - pui);

				users.get(u).setAvg(bu - alpha * gradientBu);
				items.get(i).setAvg(bi - alpha * gradientBi);
			}
			double rmse = calcRMSE();
			if (rmse > 1)
				alpha *= 1.05;
			else
				alpha *= 0.95;
			LOG.info("iterater:" + iter + "\trmse:" + rmse);
			if (lastRMSE - rmse < 0.00001)
				break;
			else
				lastRMSE = rmse;
		}
	}

	private double bPredict(int u, int i) {
		return mu + users.get(u).getAvg() + items.get(i).getAvg();
	}

	protected double predict(int u, int i, List<Double> pu) {
		return mu + users.get(u).getAvg() + items.get(i).getAvg()
				+ getProduct(Q.get(i), pu);
	}

	@Override
	protected void initParas() {
		double tot = 0d;
		for (Record record : records) {
			int userid = record.getUserId() - 1;
			int itemid = record.getMovieId() - 1;
			double rate = record.getScore();

			if (!users.containsKey(userid)) {
				users.put(userid, new RateInfo(rate, 1));
				userRates.put(userid, new HashMap<Integer, Double>());
			} else {
				users.get(userid).addRate(rate);
				userRates.get(userid).put(itemid, rate);
			}
			if (!items.containsKey(itemid))
				items.put(itemid, new RateInfo(rate, 1));
			else
				items.get(itemid).addRate(rate);

			tot += rate;
		}

		mu = tot / records.size();

		for (RateInfo bu : users.values()) {
			bu.calcAvg();
		}

		Q = new ArrayList<List<Double>>();
		X = new ArrayList<List<Double>>();
		Y = new ArrayList<List<Double>>();
		for (RateInfo bi : items.values()) {
			bi.calcAvg();
			Q.add(newRandList(feature, 0d, Math.sqrt(1.0 / feature)));
			X.add(newRandList(feature, 0d, Math.sqrt(1.0 / feature)));
			Y.add(newRandList(feature, 0d, Math.sqrt(1.0 / feature)));
		}

		LOG.info("Init Over: users=" + users.size() + " items=" + items.size());
	}

	@Override
	public String toString() {
		return "BaseLineWithMuBiBu-iter" + iterCount + "-alpha"
				+ df.format(alpha) + "-lamda" + df.format(lambda);
	}

	@Override
	protected double calcCost() {
		throw new RuntimeException("Not implemeted yet...");
	}

	@Override
	protected double predict(int userid, int itemid) {
		throw new RuntimeException("not implented");
	}
}
