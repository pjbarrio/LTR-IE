package edu.columbia.cs.ltrie.output;

import java.io.IOException;
import java.util.Set;

import edu.columbia.cs.ltrie.features.FeaturesCoordinator;

public interface OutputProducer {
	public void produceOutput(FeaturesCoordinator coordinator, Set<String> doc, Set<String> relevantDocs, String outputFile) throws IOException;
}
