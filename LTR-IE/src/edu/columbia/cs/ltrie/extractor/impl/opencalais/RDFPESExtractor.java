package edu.columbia.cs.ltrie.extractor.impl.opencalais;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gdata.util.common.base.Pair;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

import edu.columbia.cs.ltrie.datamodel.Span;
import edu.columbia.cs.ltrie.datamodel.Tuple;

public class RDFPESExtractor {

	private static final String TYPE_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String TYPE_TEXT = "type";
	
	public static List<Tuple> extract(URI uri, String relation, String docName) throws IOException{
		
		Graph g = new GraphMem();
		ModelCom model = new ModelCom(g);
		
		try {
		
		model.read(uri.toString());
		
		}
		catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		return _extract(model,relation,"r",docName);
		
	}

	public static List<Pair<Tuple,Integer>> extractWithOffset(URI uri, String relation, String docName) throws IOException{
		
		Graph g = new GraphMem();
		ModelCom model = new ModelCom(g);
		
		try {
		
		model.read(uri.toString());
		
		}
		catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		return _extractWithOffset(model,relation,"r",docName);
		
	}
	
	public static List<Tuple> _extract(ModelCom model,
			String relation, String type, String doc) {
		
		String relationText = "http://s.opencalais.com/1/type/em/"+type+"/" + relation;
		
		Node nn = Node.createURI(relationText);
		
		ResIterator resIt = model.listResourcesWithProperty(new PropertyImpl(TYPE_PROPERTY,TYPE_TEXT), new LiteralImpl(nn, model));
		
		String ReducedText = "";
		
		int tupleNumber = 0;
		
		List<Tuple> ret = new ArrayList<Tuple>();
		
		while (resIt.hasNext()){
		
			Resource res = resIt.next();
			
			Node complete = Node.createURI(res.toString());
			
			ResIterator resItComplete = model.listResourcesWithProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","subject"), new LiteralImpl(complete, model));
		
			//tuple generation
			
			Resource tuple = model.getResource(complete.getURI());
			
			StmtIterator prp = tuple.listProperties();
			
			Map<String,List<String>> tup = new HashMap<String, List<String>>();
			
			while (prp.hasNext()){
				
				Statement s = prp.next();
				
				String ss = s.getPredicate().getLocalName();
				
				if (s.getObject().isResource()){
					
					Resource auxI = model.getResource(s.getResource().getURI());
					
					StmtIterator prp2 = auxI.listProperties(new PropertyImpl("http://s.opencalais.com/1/pred/name"));
					
					while (prp2.hasNext()){ //it's only one
						
						Statement s2 = prp2.next();
						
						if (!tup.containsKey(ss)){
							tup.put(ss, new ArrayList<String>());
							tup.put("norm_" + ss,new ArrayList<String>());
						}
						
						
						tup.get(ss).add(s2.getObject().toString()); 
						tup.get("norm_" + ss).add(s2.getSubject().getURI()); 
						
						
					}
					
				} else if (s.getObject().isLiteral()){
					
					if (!tup.containsKey(ss)){
						tup.put(ss, new ArrayList<String>());
					}
					
					tup.get(ss).add(s.getObject().toString());
					
				}
				
			}
			
			List<Tuple> ts = createTuples(doc,tup);
			
			ret.addAll(ts);
			
		}
		
		return ret;
		
	}

	
	public static List<Pair<Tuple, Integer>> _extractWithOffset(ModelCom model,
			String relation, String type, String doc) {
		
		String relationText = "http://s.opencalais.com/1/type/em/"+type+"/" + relation;
		
		Node nn = Node.createURI(relationText);
		
		ResIterator resIt = model.listResourcesWithProperty(new PropertyImpl(TYPE_PROPERTY,TYPE_TEXT), new LiteralImpl(nn, model));
		
		List<Pair<Tuple,Integer>> ret = new ArrayList<Pair<Tuple,Integer>>();
		
		while (resIt.hasNext()){
		
			Resource res = resIt.next();
			
			Node complete = Node.createURI(res.toString());
			
			ResIterator resItComplete = model.listResourcesWithProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","subject"), new LiteralImpl(complete, model));
		
			//tuple generation
			
			Resource tuple = model.getResource(complete.getURI());
			
			StmtIterator prp = tuple.listProperties();
			
			Map<String,List<String>> tup = new HashMap<String, List<String>>();
			
			while (prp.hasNext()){
				
				Statement s = prp.next();
				
				String ss = s.getPredicate().getLocalName();
				
				if (s.getObject().isResource()){
					
					Resource auxI = model.getResource(s.getResource().getURI());
					
					StmtIterator prp2 = auxI.listProperties(new PropertyImpl("http://s.opencalais.com/1/pred/name"));
					
					while (prp2.hasNext()){ //it's only one
						
						Statement s2 = prp2.next();
						
						if (!tup.containsKey(ss)){
							tup.put(ss, new ArrayList<String>());
							tup.put("norm_" + ss,new ArrayList<String>());
						}
						
						
						tup.get(ss).add(s2.getObject().toString()); 
						tup.get("norm_" + ss).add(s2.getSubject().getURI()); 
						
						
					}
					
				} else if (s.getObject().isLiteral()){
					
					if (!tup.containsKey(ss)){
						tup.put(ss, new ArrayList<String>());
					}
					
					tup.get(ss).add(s.getObject().toString());
					
				}
				
			}
			
			List<Tuple> ts = createTuples(doc,tup);
			
			String offset = "";
			
			while (resItComplete.hasNext()){ //it is always only one
				
				Resource res2 = resItComplete.next();
				
				if (!res2.hasProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","exact")))
					continue;
				
				offset = res2.getProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","offset")).getObject().toString();
				
			}
			
			for (Tuple t : ts) {
			
				ret.add(new Pair<Tuple, Integer>(t, Integer.valueOf(offset)));
				
			}
			
		}
		
		return ret;
		
	}
	
	
	
	private static List<Tuple> createTuples(String doc, Map<String, List<String>> tup) {
		
		int vals = 1;
		
		for (Entry<String,List<String>> entry : tup.entrySet()) {
			
			if (!entry.getValue().isEmpty())
				vals*=entry.getValue().size();
			
		}
		
		List<Tuple> ret = new ArrayList<Tuple>(vals);
		
		for (int i = 0; i < vals; i++) {
			
			Tuple t = new Tuple();
			
			for (Entry<String,List<String>> entry : tup.entrySet()) {
				
				int index = i % entry.getValue().size();
				
				t.setData(entry.getKey(), new Span(doc, -1, -1, entry.getValue().get(index)));
								
			}
			
			ret.add(t);
			
		}

		return ret;
		
	}

	public static String extractContent(URI uri){
		
		Graph g = new GraphMem();
		ModelCom model = new ModelCom(g);
		
		try {
		
		model.read(uri.toString());
		
		}
		catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		String contentText = "http://s.opencalais.com/1/type/sys/DocInfo";
		
		Node nn = Node.createURI(contentText);
		
		ResIterator resIt = model.listResourcesWithProperty(new PropertyImpl(TYPE_PROPERTY,TYPE_TEXT), new LiteralImpl(nn, model));
		
		String exact = null;
		
		while (resIt.hasNext()){ //it is always only one
			
			Resource res2 = resIt.next();
			
			if (!res2.hasProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","document")))
				continue;
			
			exact = res2.getProperty(new PropertyImpl("http://s.opencalais.com/1/pred/","document")).getObject().toString();
			
		}

		return exact;
		
	}
	
	
}
