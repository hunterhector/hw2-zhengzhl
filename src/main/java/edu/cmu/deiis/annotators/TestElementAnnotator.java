/**
 * 
 */
package edu.cmu.deiis.annotators;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.Question;

/**
 * @author Hector
 * 
 *         This annotator annotate the questions, answers, and the scores if
 *         presented
 */
public class TestElementAnnotator extends JCasAnnotator_ImplBase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org
	 * .apache.uima.jcas.JCas)
	 */
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		String documentText = JCasUtil.selectSingle(aJCas,
				DocumentAnnotation.class).getCoveredText();
		String[] lines = documentText.split("\n");

		int lineOffset = 0;

		for (int lineNum = 0; lineNum < lines.length; lineNum++) {
			String line = lines[lineNum];

			if (line.startsWith("Q ")) {
				Question question = new Question(aJCas);
				question.setBegin(lineOffset + 2);
				question.setEnd(lineOffset + line.length());
				question.setCasProcessorId(this.getClass().getName());
				question.addToIndexes();
			}

			if (line.startsWith("A ")) {
				Answer answer = new Answer(aJCas);
				answer.setBegin(lineOffset + 4);
				answer.setEnd(lineOffset + line.length());
				answer.addToIndexes();

				String label = line.substring(2, 3);
				answer.setIsCorrect(label.equals("1"));
			}

			lineOffset += line.length() + 1;
		}
	}
}