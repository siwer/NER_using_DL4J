import org.deeplearning4j.models.embeddings.learning.impl.elements.SkipGram;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class W2V {

    //Erstellt ein W2V Modell; die boolschen Werte bestimmen, welcher Tokenizer und oder Preprocessor genutzt werden soll
    public static void createVecRepresentation (String trainPath,List<String> stopwords,int minWords,int layers,int window,int batchsize,boolean custom, boolean shape, String outputpath) throws IOException {
        Date first = new Date();
        System.out.print(first.toString() + ": Init Model...\n" + "Params:\nminWordFreq:" + minWords + " layers:" + layers + " windowSize:" + window + " stopwordlist:" + stopwords.size() + "\n");
        //Korpus,Stoppwortliste,minWordFrequency,layerSize,windowSize
        SentenceIterator iter = new BasicLineIterator("Training_Ressources/" + trainPath);
        iter.setPreProcessor((SentencePreProcessor) s -> s);
        TokenizerFactory t;
        if (custom) {
            t = new CustomTokenizerFactory();
            t.setTokenPreProcessor(new LowCaseLemmaPreProcessor());
        } if (shape) {
            t = new ShapeTokenizerFactory();
            t.setTokenPreProcessor(new CaseIgnorantPreProcessor());
        } else {
            t = new DefaultTokenizerFactory();
            t.setTokenPreProcessor(new CaseIgnorantPreProcessor());
        }
        Word2Vec vec = new Word2Vec.Builder()
                .stopWords(stopwords)
                .elementsLearningAlgorithm(new SkipGram<>())
                .batchSize(batchsize)
                .minWordFrequency(minWords)
                .iterations(1)
                .layerSize(layers)
                .seed(42)
                .windowSize(window)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();
        Date start = new Date();
        System.out.print(start.toString() + ": Fit Model...\n");
        vec.fit();
        Date end = new Date();
        System.out.print(end.toString() +": Save Model...\n");
        WordVectorSerializer.writeWord2VecModel(vec,"Training_Ressources/W2VModels/" + outputpath);
    }

    public static void checkWordvectors (Word2Vec vec,List<String> words) {
        for (String word : words) {
            System.out.print(word + ":\n" +vec.wordsNearest(word,10) + "\n");
        }
    }

    //Überführt eine Liste von Wörtern anhand des spezifizierten Modells in ihre Vektorrepräsentation
    public static List<String> wordsToVectorlist (String modelpath,List<String> words,int dimensions ,boolean dl4j) {
        Word2Vec vec;
        if (dl4j) {
            vec = WordVectorSerializer.readWord2VecModel(modelpath);
        } else {
            File gModel = new File (modelpath);
            vec = WordVectorSerializer.readWord2VecModel(gModel);
        }
        int nrOov = 0;
        double[] oov = new double[dimensions];
        List<String> vecList = new ArrayList<>();
        for (String word : words) {
            if (vec.getWordVector(word) == null) {
                vecList.add(Arrays.toString(oov));
                nrOov++;
            } else {
                vecList.add(Arrays.toString(vec.getWordVector(word)));
            }
        }
        System.out.println("OOV WORDS: " + nrOov);
        return vecList;
    }

    //Überführt genau ein Wort in seine Vektorrepräsentation
    //Modell muss vorher geladen werden
    //model = WordVectorSerializer.readWord2VecModel(Pfad)
    public static String wordToVector (Word2Vec model, String word) {
        return Arrays.toString(model.getWordVector(word));
    }
}
