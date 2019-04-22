import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;

import java.util.ArrayList;
import java.util.List;


//Der Tokenizer für die Wordshape Modelle
public class ShapeTokenizer implements Tokenizer {
    private List<String> tokens;
    private int index=0;
    private TokenPreProcess preProcess;

    public ShapeTokenizer (String input) {
        this.tokens = new ArrayList<>();
        //an einem oder mehrere Whitespacezeichen splitten
        for (String t : input.split("\\s+")) {
            if (!t.isBlank()||!t.isEmpty()) {
                this.tokens.add(t.trim());
            }
        }
    }

    @Override
    public boolean hasMoreTokens() {
        return index < tokens.size();
    }

    @Override
    public int countTokens() {
        return tokens.size();
    }

    @Override
    public String nextToken() {
        String ret = tokens.get(index);
        ret = preProcess.preProcess(ret);
        index++;
        return ret;
    }

    @Override
    public List<String> getTokens() {
        List<String> tokens = new ArrayList<>();
        while (hasMoreTokens()) {
            tokens.add(nextToken());
        }
        return tokens;
    }

    @Override
    public void setTokenPreProcessor(TokenPreProcess tokenPreProcessor) {
        this.preProcess = tokenPreProcessor;
    }

}

