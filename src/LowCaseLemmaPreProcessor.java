import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;

import java.io.IOException;

public class LowCaseLemmaPreProcessor implements TokenPreProcess {
    private Preprocessor loader = new Preprocessor();
    private Lemmatizer lemmatizer;
    {
        try {
            this.lemmatizer = this.loader.loadLemmatizer("Dictionary/LemmatizerDowncase.ser");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String preProcess(String token) {
        token = token.replaceAll("\\d","di");
        token = token.replaceAll("\\s","");
        token = token.replaceAll("(;|!|\\?|\\+|#|@|_|\"|\\)|\\(|'|`|/|…|“|–|©|\\[|]|\\{|}|\uD83D\uDE02|<|>|&|\\|;|[?]|[+]|[)]|[(]|\\|»|«|„|-|”)","");
        return lemmatizer.lemmatize(token.toLowerCase());
    }
}
