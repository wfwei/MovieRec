package baidu.zjl.util;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import baidu.entity.Record;

class Point {
	public int x;
	public int y;

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void print() {
		System.out.println("" + x + " " + y);
	}
}

public class Test{

	static void test1() {
		// 1. 先乘后四舍五入, 再除;
		double d = 62.31560027198647;

		double d2 = Math.round(d * 100) / 100.0;
		System.out.println("通过Math取整后做除法: " + d2);

		// 2. 通过BigDecimal的setScale()实现四舍五入与小数点位数确定, 将转换为一个BigDecimal对象.
		BigDecimal bd = new BigDecimal(d);
		BigDecimal bd2 = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
		System.out.println("通过BigDecimal.setScale获得: " + bd2);

		// 3. 通过DecimalFormat.format返回String的
		DecimalFormat df = new DecimalFormat("#.##");
		System.out.println("通过DecimalFormat.format获得: " + df.format(d));

		// 4. 通过String.format
		System.out.println("通过StringFormat: " + String.format("%.2f", d));

	}

	static void primitiveObjectTest() {
		HashMap<Integer, Float> userAvg = new HashMap<Integer, Float>();
		userAvg.put(1, 1f);
		Float f = userAvg.get(1);
		System.out.println(userAvg.get(1));
		f -= 1;
		System.out.println(userAvg.get(1));
	}

	static double boost(double rate) {
		double ret = rate - Math.floor(rate) - 0.5;
		ret *= 0.5 - Math.abs(ret);
		ret /= 3.1415;
		return rate + ret;
	}

	static void main(String[] args) {
		StringBuilder sb = new StringBuilder();
		sb.setCharAt(1, 'a');
		
		Point p = new Point(1, 1);
		Field[] fs = p.getClass().getFields();
		for(Field f:fs){
			System.out.println(f.toGenericString() + "\t" + f.toString());
		}
	}
}
