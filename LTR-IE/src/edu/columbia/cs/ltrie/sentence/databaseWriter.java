package edu.columbia.cs.ltrie.sentence;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.UnsupportedAddressTypeException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.gdata.util.common.base.Pair;


public class databaseWriter {

	public static final String DATABASE_USER = "user";
	public static final String DATABASE_PASSWORD = "password";
	public static final String MYSQL_AUTO_RECONNECT = "autoReconnect";
	public static final String MYSQL_MAX_RECONNECTS = "maxReconnects";

	private Connection conn;
	private String computername;
	
	private String insertSentence = "INSERT INTO `SentenceRanking`.`Sentence` (`collection`,`document`,`idSentence`,`text`) VALUES (?,?,?,?);";

	private String writeOverlapString = "INSERT INTO Overlap (idMeasure, cand_1, cand_2) VALUES (?,?,?);";
	
	
	public databaseWriter() {

		conn = null;

	}

	public synchronized void closeConnection() {
		try {
			
			getConnection().close();
			System.out.println("Disconnected from database");
			conn = null;
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}


	private synchronized Connection getConnection() {
		if (conn == null){
			openConnection();
		}

		return conn;
	}


	public synchronized  void openConnection() {

		conn = null;
		String url = "jdbc:mysql://db-files.cs.columbia.edu:3306/";
		String dbName = "SentenceRanking";
		String driver = "com.mysql.jdbc.Driver";
		String userName = "pjbarrio"; 
		String password = "test456";
		try {

			Class.forName(driver).newInstance();

			java.util.Properties connProperties = new java.util.Properties();

			connProperties.put(DATABASE_USER, userName);

			connProperties.put(DATABASE_PASSWORD, password);

			connProperties.put(MYSQL_AUTO_RECONNECT, "true");

			connProperties.put(MYSQL_MAX_RECONNECTS, "500");

			conn = DriverManager.getConnection(url+dbName,connProperties);

			System.out.println("Connected to the database");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

	


	public String getComputerName() {

		if (computername == null){
		
			try{
				computername = InetAddress.getLocalHost().getHostName();
				System.out.println(computername);
			}catch (Exception e){
				System.out.println("Exception caught ="+e.getMessage());
			}
		}
		return computername;
	}




	public void insertSentence(int collection, String name, int sentence, String sentenceText) {
		
		try {

			PreparedStatement PStmtexistsSplitForFile = getConnection().prepareStatement(insertSentence );

			PStmtexistsSplitForFile.setInt(1, collection);
			PStmtexistsSplitForFile.setString(2, name);
			PStmtexistsSplitForFile.setInt(3, sentence);
			PStmtexistsSplitForFile.setString(4, sentenceText);
			
			
			PStmtexistsSplitForFile.execute();

			PStmtexistsSplitForFile.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	
	public String getInformationExtractionSystemName(
			int idRelationExtractionSystem) {

		String ret = "";

		try {

			Statement StmtgetInformationExtractionSystemName = getConnection().createStatement();

			ResultSet RSgetInformationExtractionSystemName = StmtgetInformationExtractionSystemName.executeQuery
					("select name from RelationExtractionSystem where idRelationExtractionSystem = " + idRelationExtractionSystem);

			while (RSgetInformationExtractionSystemName.next()) {

				ret = RSgetInformationExtractionSystemName.getString(1);

			}

			RSgetInformationExtractionSystemName.close();
			StmtgetInformationExtractionSystemName.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return ret;

	}

	public int getTotalSentences(int collection) {
		
		int ret = -1;

		try {

			Statement StmtgetIts = getConnection().createStatement();

			ResultSet RSgetts = StmtgetIts.executeQuery
					("select count(*) from Sentence where collection = " + collection);

			while (RSgetts.next()) {

				ret = RSgetts.getInt(1);

			}

			RSgetts.close();
			StmtgetIts.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return ret;

		
	}

	public List<String> getSentenceText(int collection, int first, int last) {
		
		List<String> ret = new ArrayList<String>();
		
		try {

			Statement StmtgetIts = getConnection().createStatement();

			ResultSet RSgetts = StmtgetIts.executeQuery
					("select text from Sentence where collection = " + collection + " and idSentence >= " + first + " and idSentence < " + last + " order by idSentence");

			while (RSgetts.next()) {

				ret.add(RSgetts.getString(1));

			}

			RSgetts.close();
			StmtgetIts.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return ret;
		
	}

	public int getIdMeasure(String simpleName, String task) {
		
		if (simpleName.equals("CosineSimilarity")){
			if (task.equals("Train")){
				return 1;
			}else if (task.equals("Small")){
				return 2;
			}

		}
		throw new UnsupportedAddressTypeException();
		
	}

//*******************MEMORY SUPPORT**********************************//	
	
//	public boolean hasLoadedOverlap(int idMeasure) {
//		return false;
//	}
//
//	private Map<Integer,Set<Integer>> overlap_map = new HashMap<Integer,Set<Integer>>();
//	
//	public void saveOverlaps(int idMeasure, int cand_1, Set<Integer> myset) {
//		
//		overlap_map.put(cand_1, myset);
//		
//	}
//
//	public Set<Integer> getOverlap(int idMeasure, int cand_1) {
//		return overlap_map.get(cand_1);
//	}

	
	
//*******************DATABASE SUPPORT**********************************//	
	
	public void saveOverlaps(int dMeasure, int cand_1, Set<Integer> myset) {
		
		try {

		PreparedStatement PStmtwriteQueries = getConnection().prepareStatement(writeOverlapString);

			for (Integer cand_2 : myset) {
				
				PStmtwriteQueries.setInt(1, dMeasure);
				PStmtwriteQueries.setInt(2, cand_1);
				PStmtwriteQueries.setInt(3, cand_2);
				PStmtwriteQueries.addBatch();

			}

			PStmtwriteQueries.executeBatch();

			PStmtwriteQueries.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
	}

	
	public boolean hasLoadedOverlap(int idMeasure) {
		
		boolean ret = false;

		try {

			Statement StmthasLoadedOverlap = getConnection().createStatement();

			ResultSet RShasLoadedOverlap = StmthasLoadedOverlap.executeQuery
					("SELECT * from Overlap WHERE idMeasure =  " + idMeasure + ";");

			while (RShasLoadedOverlap.next()) {

				ret = true;

			}

			RShasLoadedOverlap.close();

			StmthasLoadedOverlap.close();

		} catch (SQLException e) {

			e.printStackTrace();

		}

		return ret;
		
	}

	public Set<Integer> getOverlap(int idMeasure, int cand_1) {
		
		Set<Integer> ret = new HashSet<Integer>();

		try {

			Statement StmtgetOverlap = getConnection().createStatement();

			ResultSet RSgetOverlap = StmtgetOverlap.executeQuery
					("SELECT cand_2 from Overlap WHERE idMeasure =  " + idMeasure + " and cand_1 = "+cand_1+";");

			while (RSgetOverlap.next()) {

				ret.add(RSgetOverlap.getInt(1));

			}

			RSgetOverlap.close();

			StmtgetOverlap.close();

		} catch (SQLException e) {

			e.printStackTrace();

		}

		return ret;
		
	}
//*******************DATABASE SUPPORT**********************************//

	

}
