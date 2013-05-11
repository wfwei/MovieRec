package baidu.nb;

import org.apache.log4j.Logger;

import baidu.entity.Record;

/**
 * 使用 mu+bu+bi进行预测
 * 
 * @author WangFengwei
 */
public class BaseLine extends AbstractMethod {

	protected static final Logger LOG = Logger.getLogger(BaseLine.class);

	public BaseLine(int iterCount, double alpha, double lambda) {
		this.iterCount = iterCount;
		this.alpha = alpha;
		this.lambda = lambda;
	}

	public static void main(String[] args) {
		int iterCount = 100;
		double alpha = 0.05, lamda = 0.005;
		BaseLine bl = new BaseLine(iterCount, alpha, lamda);

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
			readData(trainf);
			initParas();

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

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected double predict(int u, int i) {
		return mu + users.get(u).getAvg() + items.get(i).getAvg();
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

		for (RateInfo bu : users.values()) {
			bu.calcAvg();
		}
		for (RateInfo bi : items.values()) {
			bi.calcAvg();
		}

		LOG.info("get user:" + users.size() + " items:" + items.size());
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
}
