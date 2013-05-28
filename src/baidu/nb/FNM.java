package baidu.nb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import baidu.entity.Record;

/**
 * FNM(Factorized Neighborhood Model)<b/>
 * 
 * 使用 mu+bu+bi+qi*(xu+yu)进行预测
 * 
 * @author WangFengwei
 */
public class FNM extends AbstractMethod {

	protected int feature;

	protected HashMap<Integer, HashMap<Integer, Double>> userRates = new HashMap<Integer, HashMap<Integer, Double>>();

	protected List<List<Double>> P = new ArrayList<List<Double>>(); // userCount
																	// × feature
	protected List<List<Double>> Q = new ArrayList<List<Double>>(); // itemCount
																	// × feature
	protected List<List<Double>> X = new ArrayList<List<Double>>(); // itemCount
																	// × feature
	protected List<List<Double>> Y = new ArrayList<List<Double>>(); // itemCount
																	// × feature
	protected static final Logger LOG = Logger.getLogger(FNM.class);

	public FNM(int feature, int iterCount, double alpha, double lambda) {
		this.feature = feature;
		this.iterCount = iterCount;
		this.alpha = alpha;
		this.lambda = lambda;
	}

	public static void main(String[] args) {
		int feature = 50, iterCount = 100;
		double alpha = 0.05, lamda = 0.005;
		FNM fnm = new FNM(feature, iterCount, alpha, lamda);

		// fnm.crossValidate();
		String trainf = "resource/processedData/trainingSet";
		String predictf = "resource/processedData/predict";
		fnm.train(trainf);
		fnm.initPredict(predictf, false);
		fnm.predict();
		fnm.outputPredict("resource/zjl/BaseLine-iter" + iterCount + "-alpha"
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
			trainBui();

			// FNM训练
			trainFNM();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void trainFNM() {
		double localAlpha = alpha;

		for (int iter = 0; iter < iterCount; iter++) {
			for (Integer userid : userRates.keySet()) {

				List<Double> pu = P.get(userid);
				List<Double> pu1 = newRandList(feature, 0, 0);
				List<Double> pu2 = newRandList(feature, 0, 0);

				// Calculate pu
				double rateCoef = 0d, binCoef = 0d;
				for (Entry<Integer, Double> itemrate : userRates.get(userid)
						.entrySet()) {
					int itemid = itemrate.getKey();
					double rate = itemrate.getValue();
					double eui = rate - bPredict(userid, itemid);
					for (int f = 0; f < feature; f++) {
						pu1.set(f, pu1.get(f) + eui * X.get(itemid).get(f));
						rateCoef += Math.abs(eui); // TODO problem?
						pu2.set(f, pu2.get(f) + Y.get(itemid).get(f));
						binCoef += 1;// 1*1;
					}
				}
				rateCoef = Math.pow(rateCoef / feature, -0.5d);
				binCoef = Math.pow(binCoef, -0.5d);

				for (int f = 0; f < feature; f++) {
					pu.set(f, pu1.get(f) * rateCoef + pu2.get(f) * binCoef);
				}

				// gradient descent
				RateInfo bu = users.get(userid);
				List<Double> sum = newRandList(feature, 0, 0);
				for (Entry<Integer, Double> itemrate : userRates.get(userid)
						.entrySet()) {
					int itemid = itemrate.getKey();
					double rate = itemrate.getValue();
					double pui = predict(userid, itemid);
					double Eui = rate - pui;
					List<Double> Qi = Q.get(itemid);
					RateInfo bi = items.get(itemid);
					
					LOG.info(String.format("FNM\titer:%d\trate:%s\tpredict:%f", iter, 
							df.format(rate), pui));

					for (int f = 0; f < feature; f++) {
						sum.set(f, sum.get(f) + Eui * Qi.get(f));
					}

					for (int f = 0; f < feature; f++) {
						double gradientQif = -Eui * pu.get(f) + lambda
								* Qi.get(f);
						Qi.set(f, Qi.get(f) - localAlpha * gradientQif);

						double gradientBu = -Eui + lambda * bu.getAvg();
						bu.setAvg(bu.getAvg() - localAlpha * gradientBu);

						double gradientBi = -Eui + lambda * bi.getAvg();
						bi.setAvg(bi.getAvg() - localAlpha * gradientBi);
					}
				}

				for (Entry<Integer, Double> itemrate : userRates.get(userid)
						.entrySet()) {
					int itemid = itemrate.getKey();
					double rate = itemrate.getValue();
					List<Double> Xi = X.get(itemid);
					List<Double> Yi = Y.get(itemid);

					for (int f = 0; f < feature; f++) {
						double gradientXif = -rateCoef * (rate - bu.getAvg())
								* sum.get(f) + lambda * Xi.get(f);
						Xi.set(f, Xi.get(f) - localAlpha * gradientXif);
						double gradientYif = -binCoef * sum.get(f) + lambda
								* Yi.get(f);
						Yi.set(f, Yi.get(f) - localAlpha * gradientYif);
					}
				}

			}
			localAlpha *= 0.95;
			// calculate RMSE
			double rmse = calcRMSE();
			LOG.info("FNM\titer:" + iter + "\trmse:" + rmse);
		}
	}

	/**
	 * 使用梯度下降求解Bui
	 */
	private void trainBui() {
		double lastRMSE = 100d;
		double localAlpha = alpha;

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

				users.get(u).setAvg(bu - localAlpha * gradientBu);
				items.get(i).setAvg(bi - localAlpha * gradientBi);
			}
			double rmse = calcRMSE();
			if (rmse > 1)
				localAlpha *= 1.05;
			else
				localAlpha *= 0.95;
			LOG.info("trainBui\titer:" + iter + "\trmse:" + rmse);
			if (lastRMSE - rmse < 0.00001)
				break;
			else
				lastRMSE = rmse;
		}
	}

	private double bPredict(int u, int i) {
		return mu + users.get(u).getAvg() + items.get(i).getAvg();
	}

	@Override
	protected double predict(int userid, int itemid) {
		return bPredict(userid, itemid)
				+ getProduct(Q.get(itemid), P.get(userid));
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
			P.add(newRandList(feature, 0, 0));
		}

		for (RateInfo bi : items.values()) {
			bi.calcAvg();
			Q.add(newRandList(feature, 0d, Math.random()));
			X.add(newRandList(feature, 0d, Math.random()));
			Y.add(newRandList(feature, 0d, Math.random()));
		}

		LOG.info("Init Over: users=" + users.size() + " items=" + items.size());
	}

	@Override
	public String toString() {
		return "FNM-iter" + iterCount + "-alpha" + df.format(alpha) + "-lamda"
				+ df.format(lambda);
	}

	@Override
	protected double calcCost() {
		throw new RuntimeException("Not implemeted yet...");
	}

}
