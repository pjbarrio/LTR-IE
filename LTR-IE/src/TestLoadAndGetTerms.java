import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.util.Version;

import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.datamodel.NYTDocumentWithFields;
import edu.columbia.cs.ltrie.utils.NYTDocumentLoader;


public class TestLoadAndGetTerms {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Set<String> docs = new HashSet<String>();

		String path = "/home/goncalo/NYTValidationSplit/";
		File pathF = new File(path);
		int numPaths=pathF.list().length;
		String[] subPaths = new String[numPaths];
		for(int i=1; i<=numPaths; i++){
			subPaths[i-1]=String.format("%03d", i);
			
			File dir = new File(path + subPaths[i-1]);
			String[] files = dir.list();
			
			for(String file : files){
				docs.add(path + subPaths[i-1] + "/" + file);
			}
		}

		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_CURRENT);
		NYTCorpusDocumentParser parser = new NYTCorpusDocumentParser();
		List<DocumentWithFields> documentsWithFields = new ArrayList<DocumentWithFields>();
		int i=1;
		for(String docPath : docs){
			File f = new File(docPath);
			NYTCorpusDocument doc = parser.parseNYTCorpusDocumentFromFile(f, false);
			documentsWithFields.add(new NYTDocumentWithFields(doc));
			System.out.println(i);
			i++;
		}
	}

}
