/**
 * 
 */
package edu.cmu.deiis.annotators;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import java.util.List;
import java.util.ArrayList;

import edu.cmu.deiis.types.NGram;
import edu.cmu.deiis.types.Token;

/**
 * @author Hector
 * 
 *         This class annotate the N-Gram, assuming Tokens are already annotated
 */
public class NGramAnnotator extends JCasAnnotator_ImplBase {
	Integer[] listOfN;
	
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		// Get config. parameter values
		
		 listOfN = (Integer[]) aContext
				.getConfigParameterValue("n");
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
				int tokenIndexJ = tokenIndexI + n-1;
				if (tokenIndexJ < tokenList.size()){
					int tokenIBegin = tokenList.get(tokenIndexI).getBegin();
					int tokenJEnd = tokenList.get(tokenIndexJ).getEnd();
					NGram ngram = new NGram(aJCas);
					ngram.setBegin(tokenIBegin);
					ngram.setEnd(tokenJEnd);
					ngram.setElements(FSCollectionFactory.createFSArray(aJCas, JCasUtil.selectCovered(Token.class, ngram)));
					ngram.setElementType(Token.class.getSimpleName());
					ngram.addToIndexes();
				}
			}
		}
	}

}
