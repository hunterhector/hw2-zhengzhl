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
import edu.cmu.deiis.types.AnswerScore;
import edu.cmu.deiis.types.EntityMention;
import edu.cmu.deiis.types.NGram;
import edu.cmu.deiis.types.Question;
import edu.cmu.deiis.types.Token;

/**
 * @author hector
 * 
 *         This annotator use cosine similarities for 3 different bag of strings
 *         (Named entity, Token and NGram). While both the token weights and
 *         NGram weights are combined, the system ignore unigrams, otherwise the
 *         score will be duplicated.
 * 
 *         The current scoring is computed as followed: 0.5*token score +
 *         0.3*Named entity score + 0.2* Ngram Score
 * 
 *         The Ngram score is the average score of different ngram scores.
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
	/**
	 * The main process method get and combine the score for NER, token and
	 * NGram using cosine similarities
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

			totalScore += allNgramScore * 0.2;

			answer.setConfidence(totalScore);
			answer.setCasProcessorId(this.getClass().getName());

			// put the result into score annotation
			AnswerScore ansScore = new AnswerScore(aJCas);
			ansScore.setAnswer(answer);
			ansScore.setScore(totalScore);
			ansScore.setConfidence(totalScore);
			ansScore.setCasProcessorId(this.getClass().getName());
			ansScore.addToIndexes();
		}
	}

	/**
	 * Get cosine similarity between two bag of words, using the given vector
	 * 
	 * @param bag1
	 *            One of the bag of words
	 * @param bag2
	 *            The other bag of words
	 * @return The cosine similarities between these two bags
	 */
	private double getCosine(Map<String, Integer> bag1,
			Map<String, Integer> bag2) {
		if (bag1.isEmpty() || bag2.isEmpty()) {
			return 0;
		}

		double score = 0.0;
		for (Entry<String, Integer> tokenEntry : bag1.entrySet()) {
			String token = tokenEntry.getKey();
			Integer count = tokenEntry.getValue();
			if (bag2.containsKey(token)) {
				score += bag2.get(token) * count;
			}
		}

		return score / Math.sqrt((getLength(bag1) * getLength(bag2)));
	}

	/**
	 * Get the length of a bag of word
	 * 
	 * @param bag
	 *            the bag of word, given by word, frequency pair
	 * @return The Euclidean length calcuated by the frequency
	 */
	private double getLength(Map<String, Integer> bag) {
		double length = 0;
		for (Entry<String, Integer> tokenEntry : bag.entrySet()) {
			Integer value = tokenEntry.getValue();
			length += value * value;
		}
		return length;
	}

	/**
	 * Get the bag of annotation with frequency that is covered by the given
	 * annotation
	 * 
	 * @param annotation
	 *            The given annotation which indicates the range of the covered
	 *            types
	 * @param clazz
	 *            The class of the covered type to be counted
	 * @return The counted covered type annotation String with frequency.
	 */
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

	/**
	 * Get Ngram counts covered by the given annotation, where the different
	 * NGrams are distinguished by the N
	 * 
	 * @param annotation
	 * @return
	 */
	private <T extends Annotation> Table<Integer, String, Integer> getNgramCounts(
			T annotation) {
		Table<Integer, String, Integer> ngramCounts = HashBasedTable.create();

		for (NGram ngram : JCasUtil.selectCovered(NGram.class, annotation)) {
			Integer n = ngram.getN();
			if (n == 1) {
				// unigram is not counted here
				continue;
			}
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
