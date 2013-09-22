/**
 * 
 */
package edu.cmu.deiis.annotators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.Question;
import edu.cmu.deiis.types.Token;

/**
 * @author hector
 * 
 */
public class CosineBasedAnswerRanker extends JCasAnnotator_ImplBase {

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

		List<Token> questionTokens = JCasUtil.selectCovered(Token.class,
				question);
		Map<String, Integer> questionCounts = getTokenCounts(questionTokens);
		
		for (Answer answer : JCasUtil.select(aJCas, Answer.class)) {
			List<Token> answerTokens = JCasUtil.selectCovered(Token.class,
					answer);
			Map<String, Integer> answerCounts = getTokenCounts(answerTokens);
			double score = getCosine(questionCounts,
					answerCounts);
			answer.setConfidence(score);
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
				score += tokens2.get(token)*count;
			}
		}
		
		return score /Math.sqrt((getLength(tokens1)*getLength(tokens2)));
	}
	
	private double getLength(Map<String,Integer> tokens){
		double length = 0;
		for (Entry<String,Integer> tokenEntry : tokens.entrySet()){
			Integer value = tokenEntry.getValue();
			length += value * value;
		}
		return length;
	}

	private Map<String, Integer> getTokenCounts(List<Token> tokens) {
		Map<String, Integer> questionCounts = new HashMap<String, Integer>();
		for (Token token : tokens) {
			String text = token.getCoveredText();
			//don't count punctuations
			if (Pattern.matches("\\p{Punct}", text)) {
			    continue;
			}
			if (questionCounts.containsKey(text)) {
				questionCounts.put(text, questionCounts.get(token) + 1);
			} else {
				questionCounts.put(text, 1);
			}
		}
		return questionCounts;
	}

}
