package edu.columbia.cs.ltrie;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import weka.core.UnassignedClassException;
import edu.columbia.cs.ltrie.datamodel.Span;
import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.datamodel.ValueSpan;
import edu.columbia.cs.ltrie.excel.curves.RankingMethodCurve;
import edu.columbia.cs.ltrie.excel.curves.SortedCurve;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.utils.Pair;

public class ReadResultsMultipleFoldsSentenceCreateCSV {

	public static String NON_NORMALIZABLE = "NON_NORMALIZABLE";
	
	public final static String[] reelrelations = {"NaturalDisaster","VotingResult","ManMadeDisaster","Indictment-Arrest-Trial","PersonCareer"};

	public final static String[] relations = {"PersonParty","CompanyLocation","FamilyRelation","PersonAttributes","Extinction",
		"PoliticalRelationship","EnvironmentalIssue","PersonTravel",
		"PersonCareer","ProductRecall","CompanyLaborIssues","VotingResult","CompanyLegalIssues",
		"PersonLocation","IPO","CompanyMeeting","NaturalDisaster","CandidatePosition","Quotation","CompanyAffiliates",
		"DiplomaticRelations","ContactDetails","AnalystRecommendation","Buybacks","PatentFiling","CompanyInvestment",
		"CompanyLayoffs","Conviction","Indictment","EmploymentChange","ConferenceCall","Bankruptcy","StockSplit",
		"Dividend","CompanyCompetitor","CompanyEmployeesNumber","Trial","CompanyNameChange","DelayedFiling",
		"PoliticalEndorsement","CreditRating","BusinessRelation","BonusSharesIssuance","Acquisition",
		"CompanyForceMajeure","CompanyProduct","PersonCommunication","ArmedAttack","CompanyUsingProduct",
		"IndicesChanges","CompanyEarningsAnnouncement","MusicAlbumRelease","CompanyTechnology",
		"CompanyExpansion","CompanyFounded","AnalystEarningsEstimate","PersonEducation","PatentIssuance",
		"JointVenture","Arrest","MovieRelease","PersonEmailAddress","FDAPhase","SecondaryIssuance","GenericRelations",
		"CompanyRestatement","EquityFinancing","ManMadeDisaster","ArmsPurchaseSale","MilitaryAction","ProductIssues",
		"Alliance","DebtFinancing",
		"CompanyTicker","CompanyReorganization","CompanyAccountingChange","Merger",
		"EmploymentRelation","ProductRelease","CompanyListingChange","PersonRelation","CompanyEarningsGuidance",
		"PollsResult","CompanyCustomer"};

	public static String[] reelprintablerelatons = {"Natural Disaster-Location","Election-Winner","Man Made Disaster-Location","Person-Charge","Person-Career"};



	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {

		String t = args[0]; //"full"
		String suf = args[1]; //"Train
		String relationship = args[2]; //"Indictment-Arrest-Trial","VotingResult",/*"PersonCareer",*/"OrgAff","Outbreaks","NaturalDisaster","ManMadeDisaster"
		String extractor = args[3];
		boolean full = Boolean.valueOf(args[6]);
		boolean mmr = Boolean.valueOf(args[7]);
		
		String type = t + "_" + suf + "-sentence";

		String initialPath = "resultsRank/";

		String updateMethod = args[4]; //"ModelSimilarity","Window", "Explicit", "Smart"

		String sampling = args[5]; //"Query","Smart";

		int[] splits = new int[]{1,2,3,4,5};

		int positiontokeep = 1000;

		System.out.println("Results for " + relationship);

		CSVGenerator gen = new CSVGenerator("LTrain");
		if (full){
			getRankSVMSamInitialCurve(positiontokeep,gen,"RSVM-IE\n(Base-S)", initialPath, type, sampling, updateMethod, relationship, extractor, splits);
			getRankSVMDSInitialCurve(positiontokeep,gen,"RSVM-IE\n(Base-D)",initialPath, type, sampling, updateMethod, relationship, extractor, splits);
//			getRankSVMAdaptiveCurve(positiontokeep,gen,"RSVM-IE\n(Sent)",initialPath, type, sampling, updateMethod, relationship, extractor, splits);
		}
		if (mmr){
//			getBaggDSInitialCurve(positiontokeep,gen,"SVM\n(Sen)",initialPath, type, sampling, "Window", relationship, extractor, splits);
			getBaggDSAdaptiveCurve(positiontokeep,gen,"SVM\n(Sen)",initialPath, type, sampling, "Window", relationship, extractor, splits);
//			getRankSVMAdaptiveCurveMMR(positiontokeep,gen,"RSVM-IE\n(Doc)",initialPath, type, sampling, updateMethod, relationship, extractor, splits);
//			getRankSVMDSAdaptiveCurve(positiontokeep,gen,"RSVM-IE\n(MMR-S)",initialPath, type, sampling, updateMethod, relationship, extractor, splits);
			getRankSVMAdaptiveCurveMMR(positiontokeep,gen,"RSVM-IE\n(MMR-S)",initialPath, type, sampling, updateMethod, relationship, extractor, splits);
			getRankSVMSamAdaptiveCurve(positiontokeep,gen,"RSVM-IE\n(Adap-S)", initialPath, type, sampling, updateMethod, relationship, extractor, splits);
		}
		gen.printCSV(initialPath+ type +"/rsvm." + relationship + "." + extractor + ".");
	}

	private static void getBaggDSInitialCurve(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{

		String extr = getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve curv = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/initialBagg_SamD.data");
			gen.addCurve(name,getRelation(relationship,extr),extr,split,slimCurve(pos,curv));
			//gen.addNoveltyCurve(name, getRelation(relationship,extr),extr,split, slimNoveltyCurve(pos,curv,initialPath+ type + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMtuples_SamD.tuples"));

		}
	}
	
	private static void getRankSVMAdaptiveCurve(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{

		String extr = getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve curv = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVM.data");
			gen.addCurve(name,getRelation(relationship,extr),extr,split,slimCurve(pos,curv));
			gen.addNoveltyCurve(name, getRelation(relationship,extr),extr,split, slimNoveltyCurve(pos,curv,initialPath+ type + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMtuples.tuples"));
		}
	}

	private static void getRankSVMAdaptiveCurveMMR(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{

		String extr = getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve curv = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVM_SamD-MMR.data");
			gen.addCurve(name,getRelation(relationship,extr),extr,split,slimCurve(pos,curv));
			gen.addNoveltyCurve(name, getRelation(relationship,extr),extr,split, slimNoveltyCurve(pos,curv,initialPath+ type + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMtuples_SamD-MMR.tuples"));
		}
	}
	
	private static void getBaggDSAdaptiveCurve(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{

		String extr = getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve curv = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveBagg_SamD.data");
			gen.addCurve(name,getRelation(relationship,extr),extr,split,slimCurve(pos,curv));
			gen.addNoveltyCurve(name, getRelation(relationship,extr),extr,split, slimNoveltyCurve(pos,curv,initialPath+ type + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveBaggtuples_SamD.tuples"));
		}
	}
	
	private static void getRankSVMDSAdaptiveCurve(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{

		String extr = getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve curv = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVM_DS.data");
			gen.addCurve(name,getRelation(relationship,extr),extr,split,slimCurve(pos,curv));
			gen.addNoveltyCurve(name, getRelation(relationship,extr),extr,split, slimNoveltyCurve(pos,curv,initialPath+ type + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMtuples_DS.tuples"));
		}
	}

	private static void getRankSVMDSInitialCurve(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{

		String extr = getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve curv = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/initialRankSVM_DS.data");
			gen.addCurve(name,getRelation(relationship,extr),extr,split,slimCurve(pos,curv));
			//gen.addNoveltyCurve(name, getRelation(relationship,extr),extr,split, slimNoveltyCurve(pos,curv,initialPath+ type + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/initialRankSVMtuples_DS.tuples"));
		}
	}
	
	private static void getRankSVMSamInitialCurve(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{

		String extr = getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve curv = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/initialRankSVM_SamD.data");
			gen.addCurve(name,getRelation(relationship,extr),extr,split,slimCurve(pos,curv));
			//gen.addNoveltyCurve(name, getRelation(relationship,extr),extr,split, slimNoveltyCurve(pos,curv,initialPath+ type + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMtuples_SamD.tuples"));

		}
	}
	
	private static void getRankSVMSamAdaptiveCurve(int pos, CSVGenerator gen, String name, String initialPath, String type, String sampling, String updateMethod, String relationship, String extractor, int[] splits) throws IOException, ClassNotFoundException{

		String extr = getExtractor(extractor);

		for(int i=0; i<splits.length; i++){
			int split = splits[i];
			RankingMethodCurve curv = getCurve(initialPath+ type +"/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVM_SamD.data");
			gen.addCurve(name,getRelation(relationship,extr),extr,split,slimCurve(pos,curv));
			gen.addNoveltyCurve(name, getRelation(relationship,extr),extr,split, slimNoveltyCurve(pos,curv,initialPath+ type + "/" + sampling + "/"+ updateMethod + "/" + relationship + "/" + extractor + "/" + split + "/adaptiveRankSVMtuples_SamD.tuples"));

		}
	}

	private static Map<String,Pair<int[],int[]>> slimNoveltyCurve(int pos, RankingMethodCurve curve,String string) throws IOException, ClassNotFoundException {

		List<List<Tuple>> tuples = (List<List<Tuple>>)SerializationHelper.read(string);

		Map<String,Set<String>>tuples_atts = new HashMap<String, Set<String>>();

		//Compute Attributes

		for (List<Tuple> tupls : tuples) {

			for (Tuple tuple : tupls) {

				for (String field : tuple.getFieldNames()) {

					if (!tuples_atts.containsKey(field)){
						tuples_atts.put(field, new HashSet<String>());
						tuples_atts.put("norm_" + field, new HashSet<String>());
					}

				}

			}
		}

		Pair<int[],int[]> pair = ((SortedCurve)curve).getAbsoluteCurveRetrieval();

		int[] x = pair.first();
		int[] y = pair.second();

		Map<String,Pair<int[],int[]>> ret = new HashMap<String, Pair<int[],int[]>>();
		
		int size = (int)Math.ceil(x.length / pos);
		
		for (String att : tuples_atts.keySet()) {
			
			int[] xs = new int[size];
			int[] ys = new int[size];

			ret.put(att, new Pair<int[],int[]>(xs,ys));
			
		}
		
		ret.put("full-tuple", new Pair<int[],int[]>(new int[size],new int[size]));
		
		int tupsIndex = 0;

		int lastExtracted = 0;

		int currPos = pos;

		int index = 0;

		List<Tuple> all_unique_tuples = new ArrayList<Tuple>();
		
		for (int i = 1; i < x.length; i++) {

			if (i == currPos){

				for (Entry<String,Set<String>> atts : tuples_atts.entrySet()) {
					
					ret.get(atts.getKey()).first()[index] = i;
					ret.get(atts.getKey()).second()[index] = atts.getValue().size();
					
				}
				
				ret.get("full-tuple").first()[index] = i;
				ret.get("full-tuple").second()[index] = all_unique_tuples.size();
							
				index++;
				currPos+=pos;
			
			}

			if (y[i] > lastExtracted){
				
				lastExtracted = y[i];
				
				List<Tuple> tups = tuples.get(tupsIndex);
				
				tupsIndex++;
				
				for (Tuple tuple : tups) {

					boolean added = false;
					
					for (String field : tuple.getFieldNames()) {
						
						boolean ad = tuples_atts.get(field).add(tuple.getData(field).getValue().toLowerCase());
						
						added = added | ad;
						
					}

					if (isNovel(all_unique_tuples, tuple)){
						all_unique_tuples.add(tuple);
					}
					
					
					
				}
				
				
			}
			
			
		}

		return ret;

	}

	private static Tuple createLCTuple(Tuple tuple) {
		
		Tuple ret = new Tuple();
		
		for (String field : tuple.getFieldNames()) {
			
			Span s = tuple.getData(field);
			
			ValueSpan ns = new ValueSpan(s.getValue());
			
			ret.setData(field, ns);
						
		}
		
		return ret;
		
	}

	public static boolean isNovel(List<Tuple> all_tuples,
			Tuple tuple) {
		
		boolean novel = true;
		
		for (int i = 0; i < all_tuples.size(); i++) {
			
			if (isSameTuple(all_tuples.get(i),tuple)){
				return false;
			}
			
		}
		
		return novel;
	}
	
	private static boolean isSameTuple(Tuple existing_tuple,
			Tuple new_tuple) {
		
		if (existing_tuple.getFieldNames().size() < new_tuple.getFieldNames().size())
			return false;
		
		for (String field : new_tuple.getFieldNames()) {

			if (field.startsWith("norm_"))
				continue;
			
			Span existing_span = existing_tuple.getData(field);
			
			if (existing_span == null){
				return false;
			}else{ //The field exists

				String existing_value = existing_span.getValue();
				
				//Need to check if it refers to the same (normalized) entity
				Span new_value_sp = new_tuple.getData("norm_" + field);
				
				String new_value = new_value_sp == null? null : new_value_sp.getValue();
				
				if (new_value == null || NON_NORMALIZABLE.equals(new_value)){
					
					new_value = new_tuple.getData(field).getValue();
					
				} else{
					
					existing_value = existing_tuple.getData("norm_" + field).getValue();
					
				}
				
				//what about existing_value being non_normalizable? Next condition will say FALSE.
				
				if (!existing_value.equals(new_value)) //it's all in lowercase
					return false;

			}

		}
		
		return true;
		
	}
	
	private static RankingMethodCurve getCurve(String path) throws IOException, ClassNotFoundException{
		return (RankingMethodCurve) SerializationHelper.read(path);
	}

	static Pair<int[],int[]> slimCurve(int pos, RankingMethodCurve curve) {

		Pair<int[], int[]> results = ((SortedCurve)curve).getAbsoluteCurveRetrieval();
		int[] x = results.first();
		int[] y = results.second();

		int[] xs = new int[(int)Math.ceil(x.length / pos)];
		int[] ys = new int[(int)Math.ceil(x.length / pos)];

		int currPos;

		int index=0;

		for (currPos = pos; currPos < x.length; currPos+=pos) {

			xs[index] = x[currPos];
			ys[index] = y[currPos];

			index++;

		}

		return new Pair<int[], int[]>(xs, ys);

	}

	public static String getRelation(String relation, String extractor) {

		String[] rel = reelrelations;
		String[] report = reelprintablerelatons;

		if ("OC".equals(extractor)){
			return relation;
		}

		for (int i = 0; i < rel.length; i++) {

			if (relation.contains(rel[i])){
				return report[i];
			}

		}

		throw new UnassignedClassException("No relation");

	}

	public static String getExtractor(String extractor) {

		if (extractor.equals("OpenCalais")){
			return "OC";
		}else if (extractor.equals("Pablo-N-Grams")){
			return "BONG";
		} else if (extractor.equals("Pablo-Sub-sequences")){
			return "SSK";
		}

		throw new UnassignedClassException("No extractor");

	}

}
