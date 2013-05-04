package baidu.nb;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import baidu.entity.Record;
import baidu.zjl.util.DataUtil;

public class LFMWithBiase extends LFM {

	protected double mu;
	protected static final Logger LOG = Logger.getLogger(LFMWithBiase.class);

	public LFMWithBiase(int feature, int iterCount, double alpha, double lambda) {
		super(feature, iterCount, alpha, lambda);
	}

	public static void main(String[] args) {
		int feature = 20, iterCount = 200;
		double alpha = 0.035, lambda = 0.08;
		LFMWithBiase lfm = new LFMWithBiase(feature, iterCount, alpha, lambda);

		/* cross validate */
		// double[][] paras = { { 0.015, 0.05 }, { 0.02, 0.05 }, { 0.005, 0.05
		// },
		// { 0.01, 0.05 } };
		// for (double[] para : paras) {
		// lfm = new LFMWithBiase(feature, iterCount, para[0], para[1]);
		// double avgRMSE = lfm.crossValidate();
		// LOG.warn(lfm.toString() + "\tavgRMSE:" + avgRMSE);
		// }

		/* test */
		String trainf = "resource/processedData/trainingSet";
		String predictf = "resource/processedData/predict";
		lfm.train(trainf);
		lfm.initPredict(predictf, false);
		lfm.predict();
		String fname = "resource/zjl/" + lfm.toString();
		lfm.outputPredict(fname);
		DataUtil.getInstance().changeToUpLoadFile(fname, fname + "-upload");
	}

	@Override
	public void train(String trainf) {
		try {
			readData(trainf);
			initParas();

			double lastRMSE = 100d;

			for (int iter = 0; iter < iterCount; iter++) {
				for (Record rd : records) {
					int u = rd.getUserId() - 1;
					int i = rd.getMovieId() - 1;
					double rui = rd.getScore();
					double pui = predict(u, i);
					for (int f = 0; f < feature; f++) {

						double puf = P.get(u).get(f);
						double qif = Q.get(i).get(f);
						RateInfo bu = users.get(u);
						RateInfo bi = items.get(i);

						double gradientBu = -(rui - pui) + lambda * bu.getAvg();
						double gradientBi = -(rui - pui) + lambda * bi.getAvg();
						double gradientP = -(rui - pui) * qif + lambda * puf;
						double gradientQ = -(rui - pui) * puf + lambda * qif;

						bu.setAvg(bu.getAvg() - alpha * gradientBu);
						bi.setAvg(bi.getAvg() - alpha * gradientBi);
						P.get(u).set(f, puf - alpha * gradientP);
						Q.get(i).set(f, qif - alpha * gradientQ);
					}

				}
				double rmse = calcRMSE();
				if (rmse > 1)
					alpha *= 1.05;
				else
					alpha *= 0.95;
				// double cost = calcCost();
				LOG.info("iterater:" + iter + "\trmse:" + rmse);
				if (lastRMSE - rmse < 0.00001)
					break;
				else
					lastRMSE = rmse;
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(e.getMessage());
		}
	}

	public void predict() {
		for (Record record : predict) {
			int userid = record.getUserId() - 1;
			int itemid = record.getMovieId() - 1;
			double rate = predict(userid, itemid);
			record.setScore(rate);
		}
	}

	protected double predict(int u, int i) {
		double res = mu + users.get(u).getAvg() + items.get(i).getAvg();
		res += getProduct(P.get(u), Q.get(i));
		return res;
	}

	private double boost(double rate) {
		double ret = rate - Math.floor(rate) - 0.5;
		ret *= 0.5 - Math.abs(ret);
		return rate + ret / 3.14d;
	}

	@Override
	public double calcRMSE() {
		double rmse = 0;
		int count = 0;
		double tmp;
		for (Record rd : records) {
			tmp = rd.getScore()
					- predict(rd.getUserId() - 1, rd.getMovieId() - 1);
			rmse += tmp * tmp;
			count++;
		}
		rmse = Math.sqrt(rmse / count);
		return rmse;
	}

	@Override
	protected double calcCost() {
		double cost = 0f;

		for (Record rd : records) {
			cost += Math.pow(
					rd.getScore()
							- predict(rd.getUserId() - 1, rd.getMovieId() - 1),
					2.0);
		}

		double tmp = 0d;
		for (List<Double> p : P)
			tmp += getSquare(p);
		for (List<Double> q : Q)
			tmp += getSquare(q);
		for (RateInfo bu : users.values())
			tmp += bu.getAvg() * bu.getAvg();
		for (RateInfo bi : items.values())
			tmp += bi.getAvg() * bi.getAvg();

		cost += tmp * lambda;
		return cost;
	}

	@Override
	protected void initParas() {
		double tot = 0d;
		for (Record record : records) {
			int userid = record.getUserId() - 1;
			int itemid = record.getMovieId() - 1;
			double rate = record.getScore();

			if (!users.containsKey(userid))
				users.put(userid, new RateInfo(rate, 1));
			else
				users.get(userid).addRate(rate);
			if (!items.containsKey(itemid))
				items.put(itemid, new RateInfo(rate, 1));
			else
				items.get(itemid).addRate(rate);

			tot += rate;
		}

		mu = tot / records.size();

		P = new ArrayList<List<Double>>();
		Q = new ArrayList<List<Double>>();
		for (RateInfo bu : users.values()) {
			bu.calcAvg();
			P.add(newRandList(feature, 0d, Math.sqrt(1.0 / feature)));
		}
		for (RateInfo bi : items.values()) {
			bi.calcAvg();
			Q.add(newRandList(feature, 0d, Math.sqrt(1.0 / feature)));
		}

		LOG.info("init P&Q with feature:" + feature);
	}

	@Override
	public String toString() {
		return "LFMWithBias-fea" + feature + "-iter" + iterCount + "-alpha"
				+ df.format(alpha) + "-lambda" + df.format(lambda);
	}

}
