package baidu.nb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import baidu.entity.Record;
import baidu.zjl.util.DataUtil;

public abstract class AbstractMethod {

	protected double mu;
	protected int iterCount;
	protected double alpha;
	protected double lambda;
	protected HashMap<Integer, RateInfo> users = new HashMap<Integer, RateInfo>();
	protected HashMap<Integer, RateInfo> items = new HashMap<Integer, RateInfo>();
	protected List<Record> records = new ArrayList<Record>();
	protected List<Record> predict = new ArrayList<Record>();
	protected DecimalFormat df = new DecimalFormat("0.0000");
	DataUtil du = DataUtil.getInstance();

	private static final Logger LOG = Logger.getLogger(BaseLine.class);

	public abstract void train(String trainf);

	protected abstract double predict(int userid, int itemid);

	protected abstract void initParas();

	public abstract String toString();

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

	protected abstract double calcCost();

	public void predict() {
		for (Record record : predict) {
			int userid = record.getUserId() - 1;
			int itemid = record.getMovieId() - 1;
			double rate = predict(userid, itemid);
			record.setScore(rate);
		}
	}

	protected void readData(String dataf) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(dataf));

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
					records.add(new Record(userid, itemid, rate));
				} else
					LOG.warn("bad data:" + fields);
			}
			LOG.info("get total records:" + records.size());
		} catch (Exception e) {
			LOG.error("Fail to read data:" + e.getMessage());
			System.exit(1);
		}
	}

	protected void initPredict(String predictf, boolean withRate) {
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

	protected void outputPredict(String resultf) {
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

	protected double crossValidate() {
		double avgRMSE = 0d;
		int i;
		for (i = 0; i <= 0; i++) {
			String trainf = "resource/processedData/training-" + i;
			String predictf = "resource/processedData/testing-" + i;
			train(trainf);
			initPredict(predictf, true);

			double rmse = 0;
			int count = 0;
			for (Record record : predict) {
				int userid = record.getUserId() - 1;
				int itemid = record.getMovieId() - 1;
				double rate = record.getScore();
				double predict = predict(userid, itemid);
				rmse += Math.pow(predict - rate, 2);
				count++;
			}
			rmse = Math.sqrt(rmse / count);
			avgRMSE += rmse;
			LOG.warn(toString() + "\ti:" + i + "\tRMSE:" + rmse);
		}

		return avgRMSE / (i + 1);
	}

	protected class RateInfo {
		private double sum;
		private int count;
		private double avg;

		public RateInfo(double rate, int count) {
			this.sum = rate;
			this.count = count;
		}

		public void addRate(double rate) {
			sum += rate;
			count++;
		}

		public double calcAvg() {
			avg = sum / count;
			return avg;
		}

		public double getAvg() {
			return avg;
		}

		public void setAvg(double avg) {
			this.avg = avg;
		}

	}

	protected List<Double> newRandList(int size, double minValue,
			double maxValue) {
		List<Double> list = new ArrayList<Double>(size);
		while (size > 0) {
			list.add(minValue + Math.random() * maxValue);
			size--;
		}
		return list;
	}

	protected <T> void prt(Collection<T> l, String msg) {
		LOG.info(msg + ":\t");
		for (T t : l)
			LOG.info(t + " ");
		LOG.info("\n");
	}

	protected double getSquare(List<Double> array) {
		double sum = 0d;
		for (double d : array)
			sum += d * d;
		return sum;
	}

	protected double getProduct(List<Double> a, List<Double> b) {
		int alen = a.size();
		double sum = 0;
		for (int i = 0; i < alen; i++) {
			sum += (a.get(i) * b.get(i));
		}
		return sum;
	}
}
