package edu.columbia.cs.ltrie;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import prototype.CIMPLE.utils.CPUTimeMeasure;
import pt.utl.ist.online.learning.utils.TimeMeasurer;

import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;

import edu.columbia.cs.ltrie.datamodel.Tuple;
import edu.columbia.cs.ltrie.extractor.ExtractionSystem;
import edu.columbia.cs.ltrie.extractor.impl.cimple.CIMPLEExtractionSystem;
import edu.columbia.cs.ltrie.utils.SerializationHelper;
import edu.columbia.cs.ltrie.utils.StoredInformation;

public class ExtractResultsForDatabase {
	
	public static void main(String[] args) throws IllegalArgumentException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, SQLException {
		String extractor = args[0];
		String relationship = args[1];
		String ieSystemPath = args[2];
		String pathFile = args[3];
		String outputFile = pathFile + "_" + relationship + "Plain.data";
		
		File xpto = new File(pathFile);
		
		ExtractionSystem ieSystem = new CIMPLEExtractionSystem(ieSystemPath);
		
		TimeMeasurer measurer = new TimeMeasurer();
		File f = new File(pathFile);
		List<StoredInformation> listInfo = new ArrayList<StoredInformation>();
		double numFiles = 1;
		if(f.isDirectory()){
			File[] files = f.listFiles();
			numFiles = files.length;
			for(File doc : files){
				List<Tuple> t = ieSystem.extractTuplesFrom(doc.getAbsolutePath());
				measurer.addCheckPoint();
				if(t.size()!=0){
					listInfo.add(new StoredInformation(extractor, relationship, doc.getName(),t));
				}
			}
		}else{
			List<Tuple> t = ieSystem.extractTuplesFrom(pathFile);
			if(t.size()!=0){
				listInfo.add(new StoredInformation(extractor, relationship, f.getName(), t));
			}
			System.out.println(t);
		}
		
		//storeResults(listInfo,outputFile);
		//SerializationHelper.write("resultsTime/" + relationship + "/" + xpto.getName() + "/times.Times", measurer.getCheckPoints());
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
