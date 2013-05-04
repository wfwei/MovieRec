package baidu.nb;


public class LFMWithBiasePlus extends LFMWithBiase {

	public LFMWithBiasePlus(int feature, int iterCount, double alpha,
			double lamda) {
		super(feature, iterCount, alpha, lamda);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		return "LFMWithBiasPlus-fea" + feature + "-iter" + iterCount + "-alpha"
				+ df.format(alpha) + "-lamda" + df.format(lambda);
	}

}
