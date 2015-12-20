package edu.columbia.cs.ltrie.extractor.impl.reel;



import edu.columbia.cs.ref.model.Document;
import etxt2db.api.ClassificationExecutor;
import etxt2db.api.ClassificationExecutor.ClassifiedSpan;

import java.util.List;

import java.util.Map;



public abstract class EntityExtractor

{

	private int id;

	private Map<String,Integer> tagsId;

	private List<String> tags;

	private int tagIds[];

	protected EntityExtractor(int id, Map<String,Integer> tagsId, List<String> tags)

	{

		this.tagsId = tagsId;

		this.id = id;

		this.tags = tags;

		tagIds = new int[tags.size()];

		for(int i = 0; i < tags.size(); i++){
			tagIds[i] = ((Integer)tagsId.get(tags.get(i))).intValue();
		}


	}



	public abstract Map<String, List<ClassifiedSpan>> getClassifiedSpans(Document s);



	public int getTagId(String tag)

	{

		return ((Integer)tagsId.get(tag)).intValue();

	}



	public int getId()

	{

		return id;

	}



	protected List<String> getTags()

	{

		return tags;

	}



	public int[] getTagIds()

	{

		return tagIds;

	}





}