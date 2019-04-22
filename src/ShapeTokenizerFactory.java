import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.io.InputStream;

public class ShapeTokenizerFactory implements TokenizerFactory {
    private TokenPreProcess preProcess;

    public ShapeTokenizer create(String input) {
        ShapeTokenizer t = new ShapeTokenizer(input);
        t.setTokenPreProcessor(preProcess);
        return t;
    }

    public Tokenizer create(InputStream toTokenize) {
        throw new UnsupportedOperationException();
    }

    public void setTokenPreProcessor(TokenPreProcess preProcessor) {
        this.preProcess = preProcessor;
    }

    public TokenPreProcess getTokenPreProcessor() {
        return preProcess;
    }

}
