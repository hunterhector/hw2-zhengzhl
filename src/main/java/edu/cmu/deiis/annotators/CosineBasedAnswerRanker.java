/**
 * 
 */
package edu.cmu.deiis.annotators;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.cmu.deiis.types.Annotation;
import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.EntityMention;
import edu.cmu.deiis.types.NGram;
import edu.cmu.deiis.types.Question;
import edu.cmu.deiis.types.Token;

/**
 * @author hector
 * 
 */
public class CosineBasedAnswerRanker extends JCasAnnotator_ImplBase {
	double totalScore = 0.0;
	double documentCount = 0;

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org
	 * .apache.uima.jcas.JCas)
	 */
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		Question question = JCasUtil.selectSingle(aJCas, Question.class);

		Map<String, Integer> questionTokenCounts = getCoveredTypeCounts(
				question, Token.class);
		Map<String, Integer> questionNeCounts = getCoveredTypeCounts(question,
				EntityMention.class);
		Table<Integer, String, Integer> questionNGramCounts = getNgramCounts(question);
		Map<Integer, Map<String, Integer>> questionNGramRows = questionNGramCounts
				.rowMap();

		for (Answer answer : JCasUtil.select(aJCas, Answer.class)) {
			Map<String, Integer> answerTokenCounts = getCoveredTypeCounts(
					answer, Token.class);
			Map<String, Integer> answerNeCounts = getCoveredTypeCounts(answer,
					EntityMention.class);

			double totalScore = 0;

			double tokenScore = getCosine(questionTokenCounts,
					answerTokenCounts);
			double neScore = getCosine(questionNeCounts, answerNeCounts);
			
			totalScore += (tokenScore * 0.5 + neScore * 0.3);

			Table<Integer, String, Integer> answerNGramCounts = getNgramCounts(answer);

			int possibleNgramCount = 0;
			double allNgramScore = 0;
			for (Entry<Integer, Map<String, Integer>> answerNGramEntry : answerNGramCounts
					.rowMap().entrySet()) {
				Integer n = answerNGramEntry.getKey();
				double nGramScore = getCosine(questionNGramRows.get(n),
						answerNGramEntry.getValue());
				possibleNgramCount++;

				allNgramScore += nGramScore;
			}

			// no ngram found, give zero
			if (possibleNgramCount > 0) {
				allNgramScore /= possibleNgramCount;
			}


			System.out.println("Token score "+tokenScore);
			System.out.println("Ne score "+neScore);
			System.out.println("All ngram score "+allNgramScore);
			
			totalScore += allNgramScore * 0.2;

			answer.setConfidence(totalScore);
			answer.setCasProcessorId(this.getClass().getName());
		}
	}

	private double getCosine(Map<String, Integer> tokens1,
			Map<String, Integer> tokens2) {
		double score = 0.0;
		for (Entry<String, Integer> tokenEntry : tokens1.entrySet()) {
			String token = tokenEntry.getKey();
			Integer count = tokenEntry.getValue();
			if (tokens2.containsKey(token)) {
				score += tokens2.get(token) * count;
			}
		}

		return score / Math.sqrt((getLength(tokens1) * getLength(tokens2)));
	}

	private double getLength(Map<String, Integer> tokens) {
		double length = 0;
		for (Entry<String, Integer> tokenEntry : tokens.entrySet()) {
			Integer value = tokenEntry.getValue();
			length += value * value;
		}
		return length;
	}

	private <A extends Annotation, T extends Annotation> Map<String, Integer> getCoveredTypeCounts(
			A annotation, Class<T> clazz) {
		Map<String, Integer> annotationCounts = new HashMap<String, Integer>();
		for (T token : JCasUtil.selectCovered(clazz, annotation)) {
			String text = token.getCoveredText();
			// don't count punctuations
			if (Pattern.matches("\\p{Punct}", text)) {
				continue;
			}
			if (annotationCounts.containsKey(text)) {
				annotationCounts.put(text, annotationCounts.get(token) + 1);
			} else {
				annotationCounts.put(text, 1);
			}
		}
		return annotationCounts;
	}

	private <T extends Annotation> Table<Integer, String, Integer> getNgramCounts(
			T annotation) {
		Table<Integer, String, Integer> ngramCounts = HashBasedTable.create();

		for (NGram ngram : JCasUtil.selectCovered(NGram.class, annotation)) {
			Integer n = ngram.getN();
			String ngramText = ngram.getCoveredText();
			if (ngramCounts.contains(n, ngramText)) {
				Integer oldCount = ngramCounts.get(n, ngramText);
				ngramCounts.put(n, ngramText, oldCount + 1);
			} else {
				ngramCounts.put(n, ngramText, 1);
			}
		}

		return ngramCounts;
	}
}
