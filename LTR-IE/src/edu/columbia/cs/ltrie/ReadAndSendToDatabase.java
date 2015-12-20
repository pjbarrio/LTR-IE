package edu.columbia.cs.ltrie;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.ltrie.utils.StoredInformation;


public class ReadAndSendToDatabase {
	public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
		File results = new File("resultsOutbreaks");
		File[] files = results.listFiles();
		
		Connection conn = null;
		String userName = "ist155840";
		String password = "zrzy8261";
		String url = "jdbc:mysql://db.ist.utl.pt/ist155840";
		Class.forName ("com.mysql.jdbc.Driver").newInstance ();
		conn = DriverManager.getConnection (url, userName, password);
		
		String queryDoc = "INSERT INTO Document(path) VALUES (?) ON DUPLICATE KEY UPDATE path=path";
		PreparedStatement pstmtDoc = conn.prepareStatement(queryDoc);
		
		String queryExt = "INSERT INTO Extractor(Extractor,Relationship) VALUES (?,?) ON DUPLICATE KEY UPDATE Relationship=Relationship";
		PreparedStatement pstmtExt = conn.prepareStatement(queryExt);
		
		String queryFinal = "INSERT INTO ExtractionResults(idExtractor,idDoc,numTuples)\n" +
					   "SELECT Extractor.idExtractor, Document.id, ?\n" +
					   "FROM Document, Extractor\n" +
					   "where Document.path=?\n" +
					     "and Extractor.Extractor=?\n" +
					     "and Extractor.Relationship=?";
		PreparedStatement pstmtFinal = conn.prepareStatement(queryFinal);

		
		for(File f : files){
			System.out.println("Processing " + f.getAbsolutePath());
			List<StoredInformation> listInfo = (List<StoredInformation>) SerializationHelper.read(f.getAbsolutePath());
			for(int i=0;i<listInfo.size();i++){
				StoredInformation currentInfo = listInfo.get(i);
				executeDoc(pstmtDoc,currentInfo);
				executeExt(pstmtExt, currentInfo);
				executeFinal(pstmtFinal, currentInfo);
			}
		}
		conn.close();
	}
	
	private static void executeDoc(PreparedStatement pstmtDoc, StoredInformation currentInfo) throws SQLException{
		pstmtDoc.setString(1, currentInfo.getPathFile()); // set input parameter 1
		pstmtDoc.executeUpdate();
	}
	
	private static void executeExt(PreparedStatement pstmtExt, StoredInformation currentInfo) throws SQLException{
		pstmtExt.setString(1, currentInfo.getExtractor());
		pstmtExt.setString(2, currentInfo.getRelationship());
		pstmtExt.executeUpdate();
	}
	
	private static void executeFinal(PreparedStatement pstmtFinal, StoredInformation currentInfo) throws SQLException{
		pstmtFinal.setInt(1, currentInfo.getSize());  // set input parameter 1
		pstmtFinal.setString(2, currentInfo.getPathFile()); // set input parameter 2
		pstmtFinal.setString(3, currentInfo.getExtractor()); // set input parameter 3
		pstmtFinal.setString(4, currentInfo.getRelationship()); // set input parameter 4
		pstmtFinal.executeUpdate();
	}
}
