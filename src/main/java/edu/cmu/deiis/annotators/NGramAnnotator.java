/**
 * 
 */
package edu.cmu.deiis.annotators;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import java.util.List;
import java.util.ArrayList;

import edu.cmu.deiis.types.Token;

/**
 * @author Hector
 * 
 *         This class annotate the N-Gram, assuming Tokens are already annotated
 */
public class NGramAnnotator extends JCasAnnotator_ImplBase {
	int[] listOfN;
	
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		// Get config. parameter values
		 listOfN = (int[]) aContext
				.getConfigParameterValue("maxN");

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
		
		List <Token> tokenList = new ArrayList<Token>(JCasUtil.select(aJCas,Token.class));
	
		
		
		for (int tokenIndexI = 0;tokenIndexI< tokenList.size();tokenIndexI++){
			for (int t = 0;t<listOfN.length;t++){
				int n  = listOfN[t];
				int tokenIndexJ = tokenIndexI + n;
				if (tokenIndexI + n < tokenList.size()){
					int tokenIBegin = tokenList.get(tokenIndexI).getBegin();
					int tokenJENd = tokenList.get(tokenIndexJ).getEnd();
					
				}
				
			}
		}
	}

}
