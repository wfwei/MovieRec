package baidu.zjl.simmetrics;

public class PearsonSimilarity implements Simmetric{
	public PearsonSimilarity(){}

	@Override
	public float calcSimilariy(float[] vec1, float[] vec2) {
		float ab=0, aa=0, bb=0;
		for(int i=0; i<vec1.length; i++){
			ab += vec1[i]*vec2[i];
			aa += vec1[i]*vec1[i];
			bb +=vec2[i]*vec2[i];
		}
		return (float)(ab/(Math.sqrt(aa)*Math.sqrt(bb)));
	}
	
	@Override
	public String toString(){
		return "PearsonSimilarity";
	}
	
	public static void main(String args[]){
		float[] vec1={1,2};
		float[] vec2={1,-3};
		PearsonSimilarity sim = new PearsonSimilarity();
		System.out.println(sim.calcSimilariy(vec1, vec2));
	}
	
}
