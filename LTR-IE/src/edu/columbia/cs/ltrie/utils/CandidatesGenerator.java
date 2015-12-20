package edu.columbia.cs.ltrie.utils;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.columbia.cs.ref.model.CandidateSentence;
import edu.columbia.cs.ref.model.Document;
import edu.columbia.cs.ref.model.Sentence;
import edu.columbia.cs.ref.model.Span;
import edu.columbia.cs.ref.model.constraint.role.RoleConstraint;
import edu.columbia.cs.ref.model.entity.Entity;
import edu.columbia.cs.ref.model.relationship.Relationship;
import edu.columbia.cs.ref.model.relationship.RelationshipType;
import edu.columbia.cs.ref.tool.document.splitter.SentenceSplitter;
import edu.columbia.cs.utils.NAryCartesianProduct;

/**
 * The candidate generator is responsible for generating all the candidate sentences from a given
 * document for a given set of relationship types.
 * 
 * <br>
 * <br>
 * 
 * A candidate generator contains a splitter that will split sentences accordingly. It is important to know
 * that even though the behavior of the candidate generator is mainly driven by the sentence segmentation
 * there is one major exception: if the sentence segmentation tries to split a sentence in the middle
 * of an entity, this decision will be ignored and the two resulting sentences will become only one.
 * 
 * @author      Pablo Barrio
 * @author		Goncalo Simoes
 * @version     0.1
 * @since       2011-09-27
 */
public class CandidatesGenerator {
	private SentenceSplitter splitter;

	/**
	 * The constructor of the candidate generator. It receives a splitter that will be used to generate
	 * the sentences.
	 * 
	 * @param splitter model that generates the sentence splits
	 */
	public CandidatesGenerator(SentenceSplitter splitter){
		this.splitter=splitter;
	}

	/**
	 * This is the method that is responsible for the generation of the candidates for a given
	 * document. The generation is divided into three steps:
	 * 
	 * <br>
	 * <br>
	 * 
	 * 1) Sentence splitting: using the splitter passed in the constructor
	 * 
	 * <br>
	 * <br>
	 * 
	 * 2) Boundaries correction: in case the splitter breaks a sentence in the middle of an
	 * entity (e.g. "(...) the l.a. times (...)" may be broken as "(...) the l.a." and
	 * "time (...)"), the two resulting sentences are merged again
	 * 
	 * <br>
	 * <br>
	 * 
	 * 3) Particular candidate generation: for each sentence, the entities that are belong to
	 * it are assigned to each possible role in the relationship types in order to generate
	 * particular candidates
	 * 
	 * @param doc document from which we are trying to extract the candidates
	 * @param relationshipTypes the relationship types that we are trying to find
	 * @return the set of candidate sentences that can be generated from a document
	 */
	public Set<CandidateSentence> generateCandidates(Document doc, Set<RelationshipType> relationshipTypes){
		List<Entity> entities = new ArrayList<Entity>(doc.getEntities());
		Collections.sort(entities);
		int currentEntity=0;
		int numEntities=entities.size();

		Set<CandidateSentence> result = new HashSet<CandidateSentence>();

		List<Relationship> relationships = new ArrayList<Relationship>(doc.getRelationships());

		Sentence[] sents = splitter.split(doc);

		for(int sentId=0; sentId<sents.length; sentId++){
			Sentence sent = sents[sentId];
			Span sentenceSpan = new Span(sent.getOffset(),sent.getOffset()+sent.getLength());
			int entityIndex=currentEntity;
			while(entityIndex<numEntities){
				Entity entity=entities.get(entityIndex);
				int startEntity = entity.getOffset();
				int endEntity = entity.getOffset()+entity.getLength();
				Span entitySpan = new Span(startEntity,endEntity);
				
				while(sentenceSpan.intersects(entitySpan) && !sentenceSpan.contains(entitySpan)){
					Sentence newSentence = sents[sentId].merge(sents[sentId+1]);
					Sentence[] newSentences = new Sentence[sents.length-1];
					for(int i=0; i<sentId; i++){
						newSentences[i]=sents[i];
					}
					newSentences[sentId]=newSentence;
					for(int i=sentId+1;i<sents.length-1; i++){
						newSentences[i]=sents[i+1];
					}
					sents=newSentences;
					sent = sents[sentId];
					sentenceSpan = new Span(sent.getOffset(),sent.getOffset()+sent.getLength());
				}
				
				entityIndex++;
			}
		}
		
		currentEntity=0;
		for(int sentId=0; sentId<sents.length; sentId++){
			Sentence sent = sents[sentId];
			Span sentenceSpan = new Span(sent.getOffset(),sent.getOffset()+sent.getLength());
			Map<String,Entity> sentenceEntities = new HashMap<String,Entity>();
			Set<Relationship> sentenceRelationships = new HashSet<Relationship>();

			int entityIndex=currentEntity;
			while(entityIndex<numEntities){
				Entity entity=entities.get(entityIndex);
				int startEntity = entity.getOffset();
				int endEntity = entity.getOffset()+entity.getLength();
				Span entitySpan = new Span(startEntity,endEntity);
				if(sentenceSpan.contains(entitySpan)){
					sentenceEntities.put(entity.getId(),entity);
				}else if(sentenceSpan.intersects(entitySpan)){
					throw new UnsupportedOperationException();
				}

				if(startEntity>=sent.getOffset()+sent.getLength()){
					break;
				}
				entityIndex++;
			}
			currentEntity=entityIndex;

			//System.out.println(sentenceEntities);

			int relationshipsSize = relationships.size();
			for(int i=0; i<relationshipsSize; i++){
				Relationship rel = relationships.get(i);
				Entity[] relationshipEntities = rel.getEntities();

				if(containsAllEntities(sentenceEntities, relationshipEntities)){
					sentenceRelationships.add(rel);
					relationships.remove(i);
					relationshipsSize--;
					i--;
				}
			}

			//System.out.println(sentenceRelationships);

			result.addAll(generateCandidateSentences(doc,sent,
					new HashSet<Entity>(sentenceEntities.values()), sentenceRelationships,
					relationshipTypes));
		}

		return result;
	}

	private boolean containsAllEntities(Map<String,Entity> sentenceEntities, Entity[] entities){
		for(Entity ent : entities){
			if(!sentenceEntities.containsKey(ent.getId())){
				return false;
			}
		}
		return true;
	}

	private Set<CandidateSentence> generateCandidateSentences(
			Document doc,
			Sentence sent, Set<Entity> ents,
			Set<Relationship> sentenceRelationships,
			Set<RelationshipType> relationshipTypes) {
		Set<CandidateSentence> result = new HashSet<CandidateSentence>();

		Set<Set<Entity>> currentSets = new HashSet<Set<Entity>>();
		
		Map<List<Entity>,List<Relationship>> uniqueMaps = new HashMap<List<Entity>,List<Relationship>>();
		for(RelationshipType t : relationshipTypes){
			Set<String> roles = t.getRoles();
			Map<String,Set<Entity>> candidateEntitiesForRole = new HashMap<String,Set<Entity>>();
			for(String role : roles){
				RoleConstraint roleConstraint = t.getConstraint(role);
				Set<Entity> entitiesForRole = roleConstraint.getCompatibleEntities(ents);
				candidateEntitiesForRole.put(role, entitiesForRole);
			}
			for(Map<String,Entity> candidate : NAryCartesianProduct.generateAllPossibilities(candidateEntitiesForRole)){
				Relationship newRelationship = new Relationship(t);
				for(Entry<String,Entity> entry : candidate.entrySet()){
					newRelationship.setRole(entry.getKey(), entry.getValue());
				}

				if(sentenceRelationships.contains(newRelationship)){
					newRelationship.setLabel(t.getType());
				}else{
					newRelationship.setLabel(RelationshipType.NOT_A_RELATIONSHIP);
				}

				if(t.getRelationshipConstraint().checkConstraint(newRelationship)){
					List<Entity> entities = Arrays.asList(newRelationship.getEntities());
					Set<Entity> entitiesSet = new HashSet<Entity>(entities);
					currentSets.add(entitiesSet);
					List<Relationship> inputs = uniqueMaps.get(entities);
					if(inputs==null){
						inputs=new ArrayList<Relationship>();
					}
					inputs.add(newRelationship);
					uniqueMaps.put(entities, inputs);
				}
			}
		}
		
		for(Entry<List<Entity>,List<Relationship>> entry : uniqueMaps.entrySet()){
			CandidateSentence cand = new CandidateSentence(sent, entry.getKey());
			for(Relationship rel : entry.getValue()){
				cand.addRelationship(rel);
			}
			result.add(cand);
		}

		return result;
	}
}
