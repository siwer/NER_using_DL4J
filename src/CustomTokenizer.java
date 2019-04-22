import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;

import java.util.ArrayList;
import java.util.List;

public class CustomTokenizer implements Tokenizer {
    private List<String> tokens;
    private int index=0;
    private TokenPreProcess preProcess;

    public CustomTokenizer (String input) {
        this.tokens = new ArrayList<>();
        
        //Ersetzung von Zeichen durch Zeichen+Blank, da hieran gesplittet wird
        input = input.replaceAll(" "," ").replaceAll("((?<![^\\p{L}])\\.)",". ").
                replaceAll("((?<![^\\p{L}])\\,)",", ").replaceAll("((?<![^\\p{L}]):)",": ").
                replaceAll("((?<![^\\p{L}])/)","/ ");
        
        for (String t : input.split("((?<=\\s)|(?=\\s+)|(?<![^\\p{L}])\\.)|(?<![^\\p{L}]),|((?<![^\\p{L}]):|((?<![^\\p{L}])/))")) {
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
