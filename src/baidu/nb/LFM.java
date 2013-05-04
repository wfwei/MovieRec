package baidu.nb;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

import baidu.entity.Record;

public class LFM extends AbstractMethod {

	protected int feature;
	protected List<List<Double>> P;
	protected List<List<Double>> Q;

	protected static final Logger LOG = Logger.getLogger(LFM.class);

	public LFM(int feature, int iterCount, double alpha, double lambda) {
		this.feature = feature;
		this.iterCount = iterCount;
		this.alpha = alpha;
		this.lambda = lambda;
	}

	public static void main(String[] args) {
		int feature = 20, iterCount = 10;
		double alpha = 0.01, lambda = 0.001;

		LFM lfm = new LFM(feature, iterCount, alpha, lambda);
		double avgRMSE = lfm.crossValidate();
		LOG.warn(lfm.toString() + "\tavgRMSE:" + avgRMSE);

		// String trainf = "resource/processedData/trainingSet";
		// String predictf = "resource/processedData/predict";
		// lfm.train(trainf);
		// lfm.initPredict(predictf, false);
		// lfm.predict();
		// lfm.outputPredict("resource/zjl/LMF-WithoutBias-fea" + feature
		// + "-iter" + iterCount + "-alpha" + alpha);
	}

	public void train(String trainf) {
		try {
			readData(trainf);
			initParas();

			for (int iter = 0; iter < iterCount; iter++) {
				for (Record rd : records) {
					int u = rd.getUserId() - 1;
					int i = rd.getMovieId() - 1;
					double rui = rd.getScore();
					double pui = predict(u, i);
					for (int f = 0; f < feature; f++) {

						double puf = P.get(u).get(f);
						double qif = Q.get(i).get(f);
						double gradientP = -(rui - pui) * qif + lambda * puf;
						double gradientQ = -(rui - pui) * puf + lambda * qif;

						P.get(u).set(f, puf - alpha * gradientP);
						Q.get(i).set(f, qif - alpha * gradientQ);
					}
				}
				alpha *= 0.9;
				double rmse = calcRMSE();
				double cost = calcCost();
				LOG.info("iterater:" + iter + "\trmse:" + rmse + "\tcost:"
						+ cost);
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
		return getProduct(P.get(u), Q.get(i));
	}

	protected double calcCost() {
		double cost = 0f;

		for (Record rd : records) {
			double dif = rd.getScore()
					- predict(rd.getUserId() - 1, rd.getMovieId() - 1);
			cost += dif * dif;
		}

		double tmp = 0d;
		for (List<Double> p : P)
			tmp += getSquare(p);
		for (List<Double> q : Q)
			tmp += getSquare(q);

		cost += tmp * lambda;
		return cost;
	}

	protected void initParas() {
		for (Record record : records) {
			int userid = record.getUserId() - 1;
			int itemid = record.getMovieId() - 1;

			if (!users.containsKey(userid))
				users.put(userid, null);
			if (!items.containsKey(itemid))
				items.put(itemid, null);
		}
		LOG.info("get user:" + users.size() + " items:" + items.size());

		P = new ArrayList<List<Double>>();
		Q = new ArrayList<List<Double>>();
		for (int u = 0; u < users.size(); u++) {
			P.add(newRandList(feature, 0d, Math.sqrt(5.0 / feature)));
		}
		for (int i = 0; i < items.size(); i++) {
			Q.add(newRandList(feature, 0d, Math.sqrt(5.0 / feature)));
		}
		LOG.info("init P&Q with feature:" + feature);

	}

	public String toString() {
		return "LFM-fea" + feature + "-iter" + iterCount + "-alpha"
				+ df.format(alpha) + "-lambda" + df.format(lambda);
	}

}
