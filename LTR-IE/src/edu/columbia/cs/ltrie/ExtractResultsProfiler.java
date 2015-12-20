package edu.columbia.cs.ltrie;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.CodeSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import prototype.CIMPLE.utils.CPUTimeMeasure;
import pt.utl.ist.online.learning.utils.EmptyStatistics;
import pt.utl.ist.online.learning.utils.TimeMeasurer;

import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;

import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.impl.reel.REELRelationExtractionSystem;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.ltrie.utils.StoredInformation;

public class ExtractResultsProfiler {
	
	public static void main(String[] args) throws IllegalArgumentException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, SQLException {
		Logger.getRootLogger().removeAllAppenders();
		
		//Extractors: Shortest-Path (1), Sub-sequences (2), Bag of N-Grams (3), Dependency-Graph (4)
		//Relations: PersonCareer (1), NaturalDisaster (2), ManMadeDisaster (3), PersonTravel (4), VotingResult (5), Indictment-Arrest-Trial (6)

		
		int extractorId = Integer.parseInt(args[0]);
		int relationshipId = Integer.parseInt(args[1]);
		String pathModelsEnts = "/proj/db-files2/NoBackup/pjbarrio/models/LEARNING-TO-RANK-IE/";
		String pathModelsRE = "/proj/db-files2/NoBackup/pjbarrio/models/LEARNING-TO-RANK/";
		String pathFile = args[2] + "/"; 		//"/local/pjbarrio/Files/Downloads/NYTTrain/001/"

		
		
		System.out.println("Processing path " + pathFile);
		
		//String extractor = args[0];
		//String relationship = args[1];
		//String ieSystemPath = args[2];
		//String pathFile = "/home/goncalo/Desktop/sample";
		//String pathModelsEnts = "/home/goncalo/modelsLTR/LEARNING-TO-RANK-IE/";
		//String pathModelsRE = "/home/goncalo/modelsLTR/LEARNING-TO-RANK/";
		
		Class klass = libsvm.svm_model.class;

	    CodeSource codeSource = klass.getProtectionDomain().getCodeSource();

	    if ( codeSource != null) {

	        System.out.println("Location:" + codeSource.getLocation());

	    }

		
		REELRelationExtractionSystem ieSystem = new REELRelationExtractionSystem(pathModelsRE,pathModelsEnts,extractorId,relationshipId);
		
		String extractor = ieSystem.getExtractor();
		String relationship = ieSystem.getRelationship();
		
		File xpto = new File(pathFile);
		
		TimeMeasurer measurer = new TimeMeasurer();
		File f = new File(pathFile);
		List<StoredInformation> listInfo = new ArrayList<StoredInformation>();
		double numFiles = 1;
		if(f.isDirectory()){
			File[] files = f.listFiles();
			numFiles = files.length;
			for(int i=0; i<numFiles; i++){
				File doc = files[i];
				if (doc.getAbsolutePath().endsWith(".xml")){
					
					List<Tuple> t = ieSystem.extractTuplesFrom(doc.getAbsolutePath());
					measurer.addCheckPoint();
					if(t.size()!=0){
						listInfo.add(new StoredInformation(extractor, relationship, doc.getName(),t));
					}
					
				}
				System.out.println((i*100)/numFiles + "% of the documents processed!");
			}
		}else{
			List<Tuple> t = ieSystem.extractTuplesFrom(pathFile);
			if(t.size()!=0){
				listInfo.add(new StoredInformation(extractor, relationship, f.getName(), t));
			}
		}
		
		//String outputFile = pathFile + "_" + extractor + "_" + relationship + ".data";
		//storeResults(listInfo,outputFile);
		SerializationHelper.write("resultsTime/" + relationship + "/" + xpto.getName() + "/times.Times", measurer.getCheckPoints());
	}

	private static void storeResults(List<StoredInformation> listInfo, String path) throws SQLException, IOException {
		SerializationHelper.write(path, listInfo);
		/*Connection conn = null;	
		int i=0;
		while(true){
			try
			{
				if(conn==null){
					String userName = "ist155840";
					String password = "zrzy8261";
					String url = "jdbc:mysql://db.ist.utl.pt/ist155840";
					Class.forName ("com.mysql.jdbc.Driver").newInstance ();
					conn = DriverManager.getConnection (url, userName, password);
				}

				String query = "CALL addResults(?, ?, ?, ?)";
				PreparedStatement pstmt = conn.prepareStatement(query);
				for(;i<listInfo.size();i++){
					StoredInformation currentInfo = listInfo.get(i);
					pstmt.setString(1, currentInfo.getExtractor()); // set input parameter 1
					pstmt.setString(2, currentInfo.getRelationship()); // set input parameter 2
					pstmt.setString(3, currentInfo.getPathFile()); // set input parameter 3
					pstmt.setInt(4, currentInfo.getSize()); // set input parameter 4
					pstmt.executeUpdate();
				}
				break;
			}
			catch(MySQLNonTransientConnectionException e){
				e.printStackTrace();
				int sleepTime= (int) (Math.random()*10000);
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (SQLException e) {
				e.printStackTrace();
				int sleepTime= (int) (Math.random()*10000);
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		conn.close();*/
		
	}
}
