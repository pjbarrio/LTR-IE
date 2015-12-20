package edu.columbia.cs.ltrie.excel.curves;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import auc.ClassSort;
import auc.Confusion;
import auc.ReadList;

public class AUCComputation {
	public static double computeAUC(List<DataPoint> points) throws IOException {
		Collections.sort(points);
		Confusion localConfusion = ReadList.readFile(points);
		return localConfusion.calculateAUCROC();
	}

	public static ClassSort[] convertList(LinkedList<ClassSort> paramLinkedList) {
		ClassSort[] arrayOfClassSort = new ClassSort[paramLinkedList.size()];
		for (int i = 0; i < arrayOfClassSort.length; i++) {
			arrayOfClassSort[i] = ((ClassSort)paramLinkedList.removeFirst());
		}
		Arrays.sort(arrayOfClassSort);
		return arrayOfClassSort;
	}
}
