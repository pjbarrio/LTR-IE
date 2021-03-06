package edu.columbia.cs.ltrie.excel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import edu.columbia.cs.ltrie.excel.curves.BaselineCurve;
import edu.columbia.cs.ltrie.excel.curves.PerfectCurve;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.utils.CommandLineExecutor;
import edu.columbia.cs.utils.Pair;

public class ExcelGenerator {
	
	private static final String[] colors = {"\"blue\"","\"brown\"","\"cadetblue\"","\"chartreuse\"","\"darkgoldenrod1\"","\"darkviolet\"",
		"\"forestgreen\"","\"hotpink\"","\"deepskyblue\"","\"chocolate3\"","\"olivedrab\""};
	
	private static final int[] pch = {0,1,2,3,4,5,6,15,16,17};
	
	private static final int[] line = {1,2,3,4,5,6};
	
	private List<Pair<String,RankingMethodCurve>> curves = new ArrayList<Pair<String,RankingMethodCurve>>();
	
	private Map<String,List<Integer>> allUpdates = new HashMap<String,List<Integer>>();

	private String axisX;

	private String axisY;

	private String title;
	
	public void generateRTime(String outputFile, int numberOfPoints, boolean png,int unit, boolean pdf) throws IOException{
		
		for (int i = 0; i <=1; i++) {
			
			String f = createRFileTime(outputFile,i, numberOfPoints,png, unit, pdf);
			
			String ret = new CommandLineExecutor().getOutput("R CMD BATCH " + f + " " + f + "out");
			
			System.err.println(ret);
		}
		
	}
	
	public void generateRTimeScalability(String outputFile, int numberOfPoints, boolean png,int unit, boolean pdf) throws IOException{
		
		for (int i = 0; i <=1; i++) {
			
			String f = createRFileTimeScalability(outputFile,i, numberOfPoints,png, unit, pdf);
			
			String ret = new CommandLineExecutor().getOutput("R CMD BATCH " + f + " " + f + "out");
			
			System.err.println(ret);
		}
		
	}
	
	public void generateR(String outputFile, int numberOfPoints, boolean png, boolean pdf) throws IOException{
		
		for (int i = 0; i <=1; i++) {
			
			String f = createRFile(outputFile,i, numberOfPoints,png, pdf);
			
			String ret = new CommandLineExecutor().getOutput("R CMD BATCH " + f + " " + f + "out");
			
			System.err.println(ret);
		}
		
	}
	
	private String createRFile(String outputFile, int retrieval, int numberOfPoints, boolean png, boolean pdf) throws IOException {
		
		String suffix = (retrieval == 1)? "Retrieval":"Extraction";
		
		axisX = "Processed Documents (%)";
		
		axisY = (retrieval == 1)? "Average Recall (%)":"Tuples Recall (%)";
		
		title = (retrieval == 1)? "Document Recall by Processed Documents":"Tuples Recall by Processed Documents";
		
		String fileName = outputFile + suffix + ".R";
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
		
		bw.write("# Define 2 vectors\n");
		
		int xVals = -1;
		
		for (Pair<String,RankingMethodCurve> curve : curves) {
			if (xVals < curve.second().getCurveRetrieval().first().length);
				xVals = curve.second().getCurveRetrieval().first().length;
		}
		
		int every = 1;
		
		if (numberOfPoints > 0 && numberOfPoints < xVals){
			
			double ev = (double)xVals / (double)numberOfPoints;
			
			every = (int)Math.ceil(ev);
			
		}
		
		for (Pair<String,RankingMethodCurve> curve : curves) {
			bw.write(generateSeriesText(curve,retrieval,every,1) + "\n");
			if (allUpdates.containsKey(curve.first())){
				bw.write(generateUpdateSeries(curve,allUpdates.get(curve.first()),retrieval,every) + "\n");
			}
		}

		bw.write(generateFixedLine(xVals,1.0,numberOfPoints) + "\n");
		
		bw.write("# Calculate range from 0 to max value of cars and trucks\n");
		
		bw.write(generateRangeLine(curves) + "\n");
		
		bw.write("# Start postscript device driver to save output to " + outputFile + suffix + ".eps\n");

		bw.write(getPostcriptLine(outputFile + suffix, png, pdf) + "\n");
		
		bw.write("# Graph using y axis that ranges from 0 to max\n");
		 
		bw.write("# value in vectors.  Turn off axes and\n");
		 
		bw.write("# annotations (axis labels) so we can specify them ourself\n");
		
		bw.write(generatePlot() + "\n");
		
		bw.write("# Make x axis using labels\n");
		
		bw.write(generateAxisX(xVals,numberOfPoints) + "\n");
		
		bw.write("# Make y axis with horizontal labels that display ticks at\n");
		 
		bw.write("# every 4 marks. 4*0:g_range[2] is equivalent to c(0,4,8,12).\n");
		
		bw.write(generateAxisY(1.0) + "\n");
		
		bw.write("# Create box around plot\n");
		
		bw.write("abline(h=0.1*0:10, col=\"darkgray\", lty=2) # grid only in y-direction\n");
		
		bw.write("box()\n");

		for (int i = 0; i < curves.size(); i++) {
			
			bw.write("# Graph with solid lines\n");
			
			bw.write(generateLines(curves.get(i),i) + "\n");
			
			if (allUpdates.containsKey(curves.get(i).first())){
				bw.write(generateUpdateLine(curves.get(i).first()) + "\n");
			}
			
		}

		bw.write("# Create a title with a red, bold/italic font\n");
		
		bw.write(generateTitle() + "\n");
		
		bw.write("# Label the x and y axes with dark green text\n");

		bw.write(generateTitleX() + "\n");
		
		bw.write(generateTitleY(false) + "\n");
		
		bw.write("# Create a legend at (1, g_range[2]) that is slightly smaller\n");

		bw.write("# (cex) and uses the same line colors and points used by\n"); 
		 
		bw.write("# the actual plots\n");
		
		bw.write(generateLegend(!allUpdates.isEmpty()) + "\n");
		
		bw.write("# Turn off device driver (to flush output to EPS)\n");

		bw.write("dev.off()\n");
		
		bw.close();

		return fileName;
		
	}
	
	private String createRFileTime(String outputFile, int retrieval, int numberOfPoints, boolean png, int unit, boolean pdf) throws IOException {
		
		String suffix = (retrieval == 1)? "Retrieval":"Extraction";
		
		axisX = (retrieval == 1)? "Useful Document Recall (%)":"Tuples Recall (%)";
		
		int unitNormalization;
		if(unit==0){
			axisY = "CPU Time (s)";
			unitNormalization = 1;
		}else if(unit==1){
			axisY = "CPU Time (m)";
			unitNormalization=60;
		}else if(unit==2){
			axisY = "CPU Time (h)";
			unitNormalization=3600;
		}else{
			axisY = "CPU Time (d)";
			unitNormalization=3600*24;
		}
		
		
		title = (retrieval == 1)? "Document Recall by Processed Documents":"Tuples Recall by Processed Documents";
		
		String fileName = outputFile + suffix + ".R";
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
		
		bw.write("# Define 2 vectors\n");
		
		int xVals = -1;
		
		double maxYAxis=-1;
		for (Pair<String,RankingMethodCurve> curve : curves) {
			double[] vec = curve.second().getCurveRetrieval().second();
			maxYAxis = Math.max(maxYAxis, vec[vec.length-1]);
			if (xVals < curve.second().getCurveRetrieval().first().length)
				xVals = curve.second().getCurveRetrieval().first().length;
		}
		
		int every = 1;
		
		if (numberOfPoints > 0 && numberOfPoints < xVals){
			
			double ev = (double)xVals / (double)numberOfPoints;
			
			every = (int)Math.ceil(ev);
			
		}
		
		for (Pair<String,RankingMethodCurve> curve : curves) {
			bw.write(generateSeriesText(curve,retrieval,every,unitNormalization) + "\n");
			if (allUpdates.containsKey(curve.first())){
				bw.write(generateUpdateSeries(curve,allUpdates.get(curve.first()),retrieval,every) + "\n");
			}
		}

		bw.write(generateFixedLine(xVals,1.0,numberOfPoints) + "\n");
		
		bw.write("# Calculate range from 0 to max value of cars and trucks\n");
		
		bw.write(generateRangeLine(curves) + "\n");
		
		bw.write("# Start postscript device driver to save output to " + outputFile + suffix + ".eps\n");

		bw.write(getPostcriptLine(outputFile + suffix, png, pdf) + "\n");
		
		bw.write("# Graph using y axis that ranges from 0 to max\n");
		 
		bw.write("# value in vectors.  Turn off axes and\n");
		 
		bw.write("# annotations (axis labels) so we can specify them ourself\n");
		
		bw.write("par(mar=c(5,8,4,2))\n");
		
		bw.write(generatePlot() + "\n");
		
		bw.write("# Make x axis using labels\n");
		
		bw.write(generateAxisX(xVals,numberOfPoints) + "\n");
		
		bw.write("# Make y axis with horizontal labels that display ticks at\n");
		 
		bw.write("# every 4 marks. 4*0:g_range[2] is equivalent to c(0,4,8,12).\n");
		
		bw.write(generateAxisYTime(maxYAxis,unitNormalization) + "\n");
		
		bw.write("# Create box around plot\n");
		
		int normalized = (int) Math.ceil(maxYAxis / (10.0*unitNormalization));
		int max = normalized*10;
		bw.write("abline(h=(" + max + "*0.1)*0:10, col=\"darkgray\", lty=2) # grid only in y-direction\n");
		
		bw.write("box()\n");

		for (int i = 0; i < curves.size(); i++) {
			
			bw.write("# Graph with solid lines\n");
			
			if(i==0){
				bw.write(generateLines(curves.get(i),0) + "\n");
			}else{
				bw.write(generateLines(curves.get(i),i+1) + "\n");
			}
			
			if (allUpdates.containsKey(curves.get(i).first())){
				bw.write(generateUpdateLine(curves.get(i).first()) + "\n");
			}
			
		}

		bw.write("# Create a title with a red, bold/italic font\n");
		
		bw.write(generateTitle() + "\n");
		
		bw.write("# Label the x and y axes with dark green text\n");

		bw.write(generateTitleX() + "\n");
		
		bw.write(generateTitleY(true) + "\n");
		
		bw.write("# Create a legend at (1, g_range[2]) that is slightly smaller\n");

		bw.write("# (cex) and uses the same line colors and points used by\n"); 
		 
		bw.write("# the actual plots\n");
		
		bw.write(generateLegendTime(!allUpdates.isEmpty()) + "\n");
		
		bw.write("# Turn off device driver (to flush output to EPS)\n");

		bw.write("dev.off()\n");
		
		bw.close();

		return fileName;
		
	}
	
	private String createRFileTimeScalability(String outputFile, int retrieval, int numberOfPoints, boolean png, int unit, boolean pdf) throws IOException {
		
		String suffix = (retrieval == 1)? "Retrieval":"Extraction";
		
		axisX = "Collection Size (%)";
		
		int unitNormalization;
		if(unit==0){
			axisY = "CPU Time (s)";
			unitNormalization = 1;
		}else if(unit==1){
			axisY = "CPU Time (m)";
			unitNormalization=60;
		}else if(unit==2){
			axisY = "CPU Time (h)";
			unitNormalization=3600;
		}else{
			axisY = "CPU Time (d)";
			unitNormalization=3600*24;
		}
		
		
		title = (retrieval == 1)? "Document Recall by Processed Documents":"Tuples Recall by Processed Documents";
		
		String fileName = outputFile + suffix + ".R";
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
		
		bw.write("# Define 2 vectors\n");
		
		int xVals = -1;
		
		double maxYAxis=-1;
		for (Pair<String,RankingMethodCurve> curve : curves) {
			double[] vec = curve.second().getCurveRetrieval().second();
			maxYAxis = Math.max(maxYAxis, vec[vec.length-1]);
			if (xVals < curve.second().getCurveRetrieval().first().length)
				xVals = curve.second().getCurveRetrieval().first().length;
		}
		
		int every = 1;
		
		if (numberOfPoints > 0 && numberOfPoints < xVals){
			
			double ev = (double)xVals / (double)numberOfPoints;
			
			every = (int)Math.ceil(ev);
			
		}
		
		for (Pair<String,RankingMethodCurve> curve : curves) {
			bw.write(generateSeriesText(curve,retrieval,every,unitNormalization) + "\n");
			if (allUpdates.containsKey(curve.first())){
				bw.write(generateUpdateSeries(curve,allUpdates.get(curve.first()),retrieval,every) + "\n");
			}
		}

		bw.write(generateFixedLine(xVals,1.0,numberOfPoints) + "\n");
		
		bw.write("# Calculate range from 0 to max value of cars and trucks\n");
		
		bw.write(generateRangeLine(curves) + "\n");
		
		bw.write("# Start postscript device driver to save output to " + outputFile + suffix + ".eps\n");

		bw.write(getPostcriptLine(outputFile + suffix, png, pdf) + "\n");
		
		bw.write("# Graph using y axis that ranges from 0 to max\n");
		 
		bw.write("# value in vectors.  Turn off axes and\n");
		 
		bw.write("# annotations (axis labels) so we can specify them ourself\n");
		
		bw.write("par(mar=c(5,8,4,2))\n");
		
		bw.write(generatePlot() + "\n");
		
		bw.write("# Make x axis using labels\n");
		
		bw.write(generateAxisX(xVals,numberOfPoints) + "\n");
		
		bw.write("# Make y axis with horizontal labels that display ticks at\n");
		 
		bw.write("# every 4 marks. 4*0:g_range[2] is equivalent to c(0,4,8,12).\n");
		
		bw.write(generateAxisYTime(maxYAxis,unitNormalization) + "\n");
		
		bw.write("# Create box around plot\n");
		
		int normalized = (int) Math.ceil(maxYAxis / (10.0*unitNormalization));
		int max = normalized*10;
		bw.write("abline(h=(" + max + "*0.1)*0:10, col=\"darkgray\", lty=2) # grid only in y-direction\n");
		
		bw.write("box()\n");

		for (int i = 0; i < curves.size(); i++) {
			
			bw.write("# Graph with solid lines\n");
			
			if(i==0){
				bw.write(generateLines(curves.get(i),0) + "\n");
			}else{
				bw.write(generateLines(curves.get(i),i+1) + "\n");
			}
			
			if (allUpdates.containsKey(curves.get(i).first())){
				bw.write(generateUpdateLine(curves.get(i).first()) + "\n");
			}
			
		}

		bw.write("# Create a title with a red, bold/italic font\n");
		
		bw.write(generateTitle() + "\n");
		
		bw.write("# Label the x and y axes with dark green text\n");

		bw.write(generateTitleX() + "\n");
		
		bw.write(generateTitleY(true) + "\n");
		
		bw.write("# Create a legend at (1, g_range[2]) that is slightly smaller\n");

		bw.write("# (cex) and uses the same line colors and points used by\n"); 
		 
		bw.write("# the actual plots\n");
		
		bw.write(generateLegendTime(!allUpdates.isEmpty()) + "\n");
		
		bw.write("# Turn off device driver (to flush output to EPS)\n");

		bw.write("dev.off()\n");
		
		bw.close();

		return fileName;
		
	}

	private String generateUpdateLine(String series) {
		
		String ret = "lines(" + series + "upd, type=\"p\", pch=19, lty=1, lwd=2, col=\"black\", ,cex=1.5)";
	
		return ret;
//		lines(Incremental, type="o", pch=22, lty=2, col="red")

	}
	
	private String generateUpdateSeries(
			Pair<String, RankingMethodCurve> curve,
			List<Integer> list, int retrieval, int every) {
	
		List<Pair<Integer,Double>> plotted = getSurvivalPoints((retrieval == 1) ? curve.second().getCurveRetrieval() : curve.second().getCurveExtraction(),every);
		
		String s = curve.first().concat("upd") + " <- c(" + generateUpdateSeries(plotted,list) + ")";		
		
		return s;
		
//		MSC <- c(1.7, 2.323432, 4.6565, 18.3453, 19.00008)
		
	}
	
	private String generateUpdateSeries(List<Pair<Integer,Double>> plotted, List<Integer> list){
		
		String ret = "NA";
		
		int index = 0;
		
		for (int i = 0; i < plotted.size()-1 && index < list.size(); i++) {
			
			if (plotted.get(i+1).first()>list.get(index)){
				
				//update happened before next point.
				
				ret += "," + plotted.get(i).second();
				index++;
				
			}else{
				ret += ",NA";

			}
			
		}
		
		return ret;
	}
	
	private List<Pair<Integer,Double>> getSurvivalPoints(Pair<double[], double[]> series, int every) {
		
		List<Pair<Integer,Double>> ret = new ArrayList<Pair<Integer,Double>>();
		
		double[] yValues = series.second();
		
		if (yValues.length == 0){
			return ret;
		}
		
		for (int i = 0; i < yValues.length; i++) {
			
			if (i % every == 0){
				
				ret.add(new Pair<Integer,Double>(i, yValues[i]));
				
			}
		}
		
		return ret;
	}
	
	private String generateFixedLine(int series, double maxValue, int numberOfPoints) {
		
		String ret = "Fixed <- c("+maxValue ;//+ maxValue;
		
		for (int i = 2; i < Math.min(series, (numberOfPoints < 0 )? series : numberOfPoints); i++) {
			
			ret += "," + maxValue;
			
		}
		
		return ret + ")";
	
	}

	private String generateLegend(boolean hasUpdate) {
		
		String ret = "legend(\"bottomright\", c(\"" + curves.get(0).first().replace("__", "-").replace("_", " ") + "\"";
		for (int i = 1; i < curves.size(); i++) {
			
			ret += ",\"" + curves.get(i).first().replace("__", "-").replace("_", " ") + "\"";
			
		}
		if (hasUpdate)
			ret += ",\"Update\"";
		
		String color = "col=c(" + getColor(0);
		String lty = "lty=c(" + getLty(0);
		String pch = "pch=c(" + getPch(0);
		
		for (int i = 1; i < curves.size(); i++) {
			
			color += "," + getColor(i);
			lty += "," + getLty(i);
			pch += "," + getPch(i);
						
		}
		if (hasUpdate){
			color += ",\"black\"";
			lty += ",NA";
			pch +=",19";
		}
		
		color += ")";
		lty += ")";
		pch += ")";
		
		ret += "), cex=1.4,  " + color + ", " + pch + ", " + lty + ", lwd = 2, bg = \"white\");";
		return ret;
	}
	
	private String generateLegendTime(boolean hasUpdate) {
		
		String ret = "legend(\"topleft\", c(\"" + curves.get(0).first().replace("__", "-").replace("_", " ") + "\"";
		for (int i = 1; i < curves.size(); i++) {
			
			ret += ",\"" + curves.get(i).first().replace("__", "-").replace("_", " ") + "\"";
			
		}
		if (hasUpdate)
			ret += ",\"Update\"";
		
		String color = "col=c(" + getColor(0);
		String lty = "lty=c(" + getLty(0);
		String pch = "pch=c(" + getPch(0);
		
		for (int i = 1; i < curves.size(); i++) {
			
			color += "," + getColor(i+1);
			lty += "," + getLty(i+1);
			pch += "," + getPch(i+1);
						
		}
		if (hasUpdate){
			color += ",\"black\"";
			lty += ",NA";
			pch +=",19";
		}
		
		color += ")";
		lty += ")";
		pch += ")";
		
		ret += "), cex=1.4,  " + color + ", " + pch + ", " + lty + ", lwd = 2, bg = \"white\");";
		return ret;
	}

	private int getLty(int i) {
		return line[(int)i % line.length];
	}

	private int getPch(int i) {
		return pch[(int)i % pch.length];
	}

	private String getColor(int i) {
		
		//XXX add more colors?
		
		return colors[(int)i % colors.length];
	}

	private String generateTitleY(boolean isTime) {
		
		String size = "";
		if(isTime){
			size="\\n\\n";
		}
		
		String ret = "title(ylab=\"" + axisY + size + "\", col.lab=\"black\", cex.lab = 1.5)";
		
//		title(ylab="Test Y", col.lab=rgb(0,0.5,0))

		return ret;
		
	}

	private String generateTitleX() {
		
		String ret = "title(xlab=\"" + axisX + "\", col.lab=\"black\", cex.lab = 1.5)";

		return ret;
		
//		title(xlab="Test X", col.lab=rgb(0,0.5,0))
	}

	private String generateTitle() {
		
		String ret = "#title(main=\"" + title + "\", col.main=\"black\", font.main=4)"; //XXX is commented, since it appears in the text
		
//		title(main="Title", col.main="red", font.main=4)

		return ret;
	}

	private String generateLines(Pair<String,RankingMethodCurve> series, int i) {
	
		String ret = "lines(" + series.first() + ", type=\"o\", pch="+getPch(i)+", lty="+getLty(i)+", lwd=2, col=" + getColor(i) + ", cex=1.6)";
	
		return ret;
//		lines(Incremental, type="o", pch=22, lty=2, col="red")

	}

	private String generateAxisY(double maxValue) {
//		return "axis(2, las=1, at=0.1*0:g_range[2])";
		
		double normalized = maxValue / 10.0;
		
		return "axis(2, las=1, at="+normalized+"*0:10" + getLabelsY() + ", cex.axis=1.5)";
	
	}
	
	private String generateAxisYTime(double maxValue, int unitNormalization) {
		int normalized = (int) Math.ceil(maxValue / (10.0*unitNormalization));
		int max = normalized*10;
		
		DecimalFormat df =  new DecimalFormat();
		df.setGroupingSize(3);
		String labelsY = ", lab=c(\"0\"";
		for (int i = 1; i <= 10; i++) {
			labelsY+=",\"" + df.format(i*normalized) + "\"";
		}
		
		labelsY+= ")";
		
		System.out.println("axis(2, las=1, at="+normalized+"*0:10"  + labelsY +", cex.axis=1.5)");
		return "axis(2, las=1, at="+normalized+"*0:10"  + labelsY +", cex.axis=1.5)";
	
	}

	private String getLabelsY() {
		
		String ret = ", lab=c(\"0\"";
		
		for (int i = 10; i <= 100; i+=10) {
			ret+=",\"" + i + "\"";
		}
		
		return ret + ")";
		
	}

	private String generateAxisX(int series, int numberOfPoints) {
		
		double initial = ((double)numberOfPoints-1.0) / 10.0;
		
		double offset = (((double)numberOfPoints-1.0) % 10.0) / 10.0;
		
		String ret = "axis(1, at="+initial+"*0:10 + " + offset;
		
		ret += getLabelsY() + ", cex.axis=1.5)";
		
//		axis(1, at=1:8, lab=c("1","3","4","30","35","36","38","50"))

		return ret;
		
	}

	private String generateIntervals(int xValues) {
				
		DecimalFormat df = new DecimalFormat("%#.#####");
		
		String ret = "\""+df.format(0.0)+"\"";
			
		for (int xVal = 1; xVal <= xValues ; xVal++) {
			
			ret += ",\"" + df.format((double)xVal/(double)xValues) + "\"";
			
		}
		
		if (ret.isEmpty())
			return ret;
		
		return ret;
	
	}

	private String generatePlot() {
		
		String ret = "plot(Fixed, type=\"n\", col=" + getColor(0) + ", ylim=g_range, lwd = 2, axes=FALSE, ann=FALSE)"; 
		
//		plot(MSC, type="o", col="blue", ylim=g_range, 
//				axes=FALSE, ann=FALSE)
	
		return ret;
	}

	private String getPostcriptLine(String outPutName, boolean png, boolean pdf) {
		if (png){
			return "png(file=\"" + outPutName + ".png\", type=\"cairo\", width=8, height=5, units=\"in\", pointsize=12, res=96*16)";
		}else if (pdf){
			return "pdf(file=\"" + outPutName + ".pdf\", width=8, height=5, onefile=FALSE)";
		}else{
			return "postscript(file=\"" + outPutName + ".eps\", width=8, height=5, horizontal = FALSE, onefile=FALSE)";
		}
		
	}

	private String generateRangeLine(List<Pair<String,RankingMethodCurve>> seriesData) {
		
		String ret = "g_range <- range(0";
		
		for (Pair<String,RankingMethodCurve> series : seriesData) {
			ret += ", " + series.first(); 
		}
		
		return ret + ", Fixed)";
//		g_range <- range(0, MSC, Incremental,QProber)
	}

	private String generateSeriesText(Pair<String,RankingMethodCurve> series, int retrieval, int every, int unitNormalization) {
		String ret = series.first() + " <- c(0.0" + getYInterval((retrieval == 1) ? series.second().getCurveRetrieval() : series.second().getCurveExtraction(),every, unitNormalization) + ")";
		
		return ret;
		
//		MSC <- c(1.7, 2.323432, 4.6565, 18.3453, 19.00008)
//		Incremental <- c(2, 3, 4, 5, 12)
//		QProber <- c(12, 3, 14, 5, 12)

	}

	private String getYInterval(Pair<double[], double[]> series, int every, int unitNormalization) {
		
		String ret = "";
		
		double[] yValues = series.second();
		
		if (yValues.length == 0){
			return ret;
		}
		
		for (int i = 1; i < yValues.length; i++) {
			
			if (i % every == 0)
				ret += "," + yValues[i]/unitNormalization;

		}
		
		return ret;
	}
	
	public void generateExcel(String outputFile) throws IOException, RowsExceededException, WriteException{
		File file = new File(outputFile);
		WorkbookSettings wbSettings = new WorkbookSettings();
		
		wbSettings.setLocale(new Locale("en", "EN"));
		WritableWorkbook workbook = Workbook.createWorkbook(file, wbSettings);
		workbook.createSheet("Results Retrieval", 0);
		workbook.createSheet("Results Extraction", 1);
		WritableSheet sheetRet = workbook.getSheet(0);
		createCurvesSheet(sheetRet, true);
		WritableSheet sheetExt = workbook.getSheet(1);
		createCurvesSheet(sheetExt, false);
		
		
		workbook.write();
		workbook.close();
	}
	
	private void createCurvesSheet(WritableSheet sheet, boolean retrieval) throws RowsExceededException, WriteException{
		int sizeCurves = curves.size();
		int numRows=-1;
		for(int column=0; column<sizeCurves; column++){
			Pair<String,RankingMethodCurve> curvePair = curves.get(column);
			Pair<double[],double[]> curve;
			if(retrieval){
				curve=curvePair.second().getCurveRetrieval();
			}else{
				curve=curvePair.second().getCurveExtraction();
			}
			if(column==0){
				addLabel(sheet, 0, 0, "Processed Documents");
				double[] xValues = curve.first();
				numRows=Math.min(xValues.length,64000);
				for(int row=0; row<numRows; row++){
					addNumber(sheet, row+1, 0, xValues[row]);
				}
			}
			
			double[] yValues = curve.second();
//			if(yValues.length==numRows){
				for(int row=0; row<numRows; row++){
					if(yValues[row]!=Double.MIN_VALUE){
						addNumber(sheet, row+1, column+1, yValues[row]);
					}
				}
//			}else{
//				throw new UnsupportedOperationException("The number of points is not the same for every curve");
//			}
			
			addLabel(sheet, 0, column+1, curvePair.first());
		}
	}
	
	private void addNumber(WritableSheet sheet, int row, int column,
			double integer) throws WriteException, RowsExceededException {
		Number number;
		number = new Number(column, row, integer);
		sheet.addCell(number);
	}

	private void addLabel(WritableSheet sheet, int row, int column, String s)
			throws WriteException, RowsExceededException {
		Label label;
		label = new Label(column, row, s);
		sheet.addCell(label);
	}
	
	public void addRankingCurve(String name, RankingMethodCurve curve){
		Pair<String, RankingMethodCurve> pair = new Pair<String, RankingMethodCurve>(name, curve);
		curves.add(pair);
	}
	
	/**
	 * Must use the same name than the RankingCurve!
	 * @param name Same than the original RankingCurve.
	 * @param curve
	 */
	
	public void addUpdatePoints(String name, List<Integer> updates){
		allUpdates.put(name,updates);
	}
	
	public static void main(String[] args) throws IOException, RowsExceededException, WriteException {
		ExcelGenerator gen = new ExcelGenerator();
		gen.addRankingCurve("Random Ranking", new BaselineCurve(1000));
		List<String> sortedList = new ArrayList<String>();
		Map<String,Integer> relevantDocuments = new HashMap<String,Integer>();
		for(int i=0; i<50; i++){
			relevantDocuments.put(i +".txt", 5-(i/10));
			if(i%10!=0){
				sortedList.add(i +".txt");
			}else{
				sortedList.add("other" + i + ".txt");
			}
		}
		
		for(int i=0; i<5; i++){
			sortedList.add(i + ".txt");
		}
		gen.addRankingCurve("Perfect Reanking", new PerfectCurve(1000, relevantDocuments));
		gen.addRankingCurve("Sorted", new SortedCurve(1000, sortedList, relevantDocuments));
		gen.generateExcel("testExcel.xls");
	}
	
	/*protected class RelevanceAndScoreInformation implements Comparable<RelevanceAndScoreInformation>{

		private double score;
		private int relevance;
		private String document;
		
		public RelevanceAndScoreInformation(String document, int relevance, double score){
			this.document=document;
			this.score=score;
			this.relevance=relevance;
		}
		
		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public int getRelevance() {
			return relevance;
		}

		public void setRelevance(int relevance) {
			this.relevance = relevance;
		}

		public String getDocument() {
			return document;
		}

		public void setDocument(String document) {
			this.document = document;
		}
		
		@Override
		public int compareTo(RelevanceAndScoreInformation o) {
			return (int) -Math.signum(score-o.score);
		}
		
	}*/
}
