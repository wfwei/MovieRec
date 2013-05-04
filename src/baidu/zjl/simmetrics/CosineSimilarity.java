package baidu.zjl.simmetrics;

import java.util.HashMap;

public class CosineSimilarity implements Simmetric {
	public CosineSimilarity() {
	}

	@Override
	public float calcSimilariy(float[] vec1, float[] vec2) {
		float ab = 0, aa = 0, bb = 0;
		for (int i = 0; i < vec1.length; i++) {
			ab += vec1[i] * vec2[i];
			aa += vec1[i] * vec1[i];
			bb += vec2[i] * vec2[i];
		}
		return (float) (ab / (Math.sqrt(aa) * Math.sqrt(bb)));
	}

	public static float calcSim(HashMap<Integer, Float> itema,
			HashMap<Integer, Float> itemb) {
		float ab = 0, aa = 0, bb = 0;
		for (Integer i : itema.keySet()) {
			aa += itema.get(i) * itema.get(i);
			if (itemb.containsKey(i)) {
				ab += itema.get(i) * itemb.get(i);
			}
		}
		for (Integer i : itemb.keySet()) {
			bb += itemb.get(i) * itemb.get(i);
		}
		if (ab != 0)
			return (float) (ab / (Math.sqrt(aa) * Math.sqrt(bb)));
		else
			return 0;
	}

	@Override
	public String toString() {
		return "CosineSimilarity";
	}

	public static void main(String args[]) {
		float[] vec1 = { 4, 4, 5 };
		float[] vec2 = { 1, 1, 2 };
		float[] vec3 = { 4, 1, 5 };
		CosineSimilarity sim = new CosineSimilarity();
		System.out.println("1,2:" + sim.calcSimilariy(vec1, vec2));
		System.out.println("1,3:" + sim.calcSimilariy(vec1, vec3));
		System.out.println("2,3:" + sim.calcSimilariy(vec2, vec3));
	}

}
