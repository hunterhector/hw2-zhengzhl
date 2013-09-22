/**
 * 
 */
package edu.cmu.deiis.annotators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.cmu.deiis.types.Answer;

/**
 * @author hector
 * 
 * This class annotate the precision at N for questions
 *
 */
public class PrecisionAtNEvaluator extends JCasAnnotator_ImplBase {

	/* (non-Javadoc)
	 * @see org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.apache.uima.jcas.JCas)
	 */
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		List<Answer> answers = new ArrayList<Answer>( JCasUtil.select(aJCas,Answer.class));
		double totalCorrect = 0.0;
		
		for (Answer answer : answers){
			if (answer.getIsCorrect()){
				totalCorrect += 1;
			}
		}
		

		Collections.sort(answers, new Comparator<Answer>(){
			@Override
			public int compare(Answer ans1, Answer ans2){
				return ans1.getConfidence() - ans2.getConfidence() > 0 ? 1 : 0;
			}
		});
		
		int numCorrect = 0;
		
		for (int i = 0; i< totalCorrect;i++){
			if (answers.get(i).getIsCorrect()){
				numCorrect ++;
			}
		}
		
		System.out.println("Precision at n is "+numCorrect/totalCorrect);
		
	}

}
