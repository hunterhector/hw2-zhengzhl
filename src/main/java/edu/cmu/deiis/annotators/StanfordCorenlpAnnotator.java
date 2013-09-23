/**
 * 
 */
package edu.cmu.deiis.annotators;

import java.util.Properties;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import edu.cmu.deiis.types.EntityMention;
import edu.cmu.deiis.types.Sentence;
import edu.cmu.deiis.types.Token;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author Hector
 * 
 */
public class StanfordCorenlpAnnotator extends JCasAnnotator_ImplBase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org
	 * .apache.uima.jcas.JCas)
	 */
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		String documentText = aJCas.getDocumentText();
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		// props.put("annotators", "tokenize");

		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		Annotation document = new Annotation(documentText);
		pipeline.annotate(document);

		// The following put token annotation to CAS
		String preNe = "";
		int neBegin = 0;
		int neEnd = 0;

		// Iterate through the Stanford sentences
		for (CoreMap sent : document.get(SentencesAnnotation.class)) {
			int sentBegin = sent.get(CharacterOffsetBeginAnnotation.class);
			int sentEnd = sent.get(CharacterOffsetEndAnnotation.class);

			Sentence sSent = new Sentence(aJCas, sentBegin, sentEnd);
			sSent.addToIndexes();

			// Iterate throught the Stanford Tokens
			for (CoreLabel token : sent.get(TokensAnnotation.class)) {
				int beginIndex = token
						.get(CharacterOffsetBeginAnnotation.class);
				int endIndex = token.get(CharacterOffsetEndAnnotation.class);

				Token sToken = new Token(aJCas, beginIndex, endIndex);
				sToken.setPos(token.get(PartOfSpeechAnnotation.class));
				sToken.setLemma(token.get(LemmaAnnotation.class));
				sToken.addToIndexes(aJCas);

				// Add NER annotation
				String ne = token.get(NamedEntityTagAnnotation.class);
				if (ne != null) {
					// System.out.println("[" + token.originalText() + "] :" +
					// ne);
					if (ne.equals(preNe) && !preNe.equals("")) {

					} else if (preNe.equals("")) {
						// if the previous is start of sentence(no label).
						neBegin = beginIndex;
						preNe = ne;
					} else {
						if (!preNe.equals("O")) {// "O" represent no label
							EntityMention sne = new EntityMention(aJCas);
							sne.setBegin(neBegin);
							sne.setEnd(neEnd);
							sne.setEntityType(preNe);
							// sne.setEntitySpan(documentText.substring(neBegin,neEnd));
							sne.addToIndexes(aJCas);
						}
						// set the next span of NE
						neBegin = beginIndex;
						preNe = ne;
					}
					neEnd = endIndex;
				}
			}
		}

	}
}
