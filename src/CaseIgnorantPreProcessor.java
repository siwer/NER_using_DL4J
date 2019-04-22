import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;

public class CaseIgnorantPreProcessor implements TokenPreProcess {

    //Gibt ein unverändertes Token zurück
    public String preProcess (String token) {
        return token;
    }
}
