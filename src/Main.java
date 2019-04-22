import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Date date = new Date();
        System.out.println(date);
        //Korpusumformung
        Preprocessor.corpusToCsv("Korpus/test.avro","Training_Ressources/NETest.csv","Training_Ressources/TextTest.csv");
        Preprocessor.corpusToCsv("Korpus/train.avro","Training_Ressources/NETrain.csv","Training_Ressources/TextTrain.csv");
        Preprocessor.createRawInputData("Training_Ressources/TextTest.csv","Training_Ressources/NETest.csv","Training_Ressources/TestDATA.csv");
        Preprocessor.createRawInputData("Training_Ressources/TextTrain.csv","Training_Ressources/NETrain.csv","Training_Ressources/TrainDATA.csv");
        //Erstellung der InputDaten
        Preprocessor a = new Preprocessor();
        //Laden des Lemmatizers
        Lemmatizer lemma = a.loadLemmatizer("Dictionary/LemmatizerDowncase.ser");
        //Laden des W2V Modells
        Word2Vec model = WordVectorSerializer.readWord2VecModel("Training_Ressources/W2VModels/300Dim_5Context_Model_true");
        W2VAnalyzer.getOOVWords("Training_Ressources/TestDATA.csv",model,lemma,"FinalTest",true);
        W2VAnalyzer.getOOVWords("Training_Ressources/TrainDATA.csv",model,lemma,"FinalTrain",true);
        //Splitten für Rnn
        Preprocessor.splitData("Training_Ressources/FinalTest.csv","Training_Ressources/RnnData/TestRead/");
        Preprocessor.splitData("Training_Ressources/FinalTrain.csv","Training_Ressources/RnnData/TrainRead/");
        //Laden der ShapeModelle
        Word2Vec shape = WordVectorSerializer.readWord2VecModel("Training_Ressources/W2VModels/Wordshape25Dim");
        Word2Vec shortshape = WordVectorSerializer.readWord2VecModel("Training_Ressources/W2VModels/Shortshape25Dim");
        //Überführen des Inputs in Vektoren
        VectorComposer.transformAllFolder("Training_Ressources/RnnData/TestRead/","Training_Ressources/RnnData/Test/",model,1,1,true,shape,shortshape, false,false,true,1,1,false);
        VectorComposer.transformAllFolder("Training_Ressources/RnnData/TrainRead/","Training_Ressources/RnnData/Train/",model,1,1,true,shape,shortshape, false,false,true,1,1,true);
        //Überführen in eine Datei für FF
        DataTransformer.rnnDataToFF("Training_Ressources/RnnData/Test/","Dim_925","Training_Ressources/RnnData/TestRead/","LABEL","Test925Dim",1191);
        DataTransformer.rnnDataToFF("Training_Ressources/RnnData/Train/","Dim_925","Training_Ressources/RnnData/TrainRead/","LABEL","Train925Dim",1243);
        //FeedForward Durchlauf mit den Daten
        NeuralNetwork.feedForwardTest("Training_Ressources/Train925Dim.csv","Training_Ressources/Test925Dim.csv",925,40,2);
        //Rnn Durchlauf mit den Daten
		//Absoluter Pfad zu RnnData wie z.B.: C:/.../.../Programm/Training_Ressources/RnnData
		String absolutePath = "";
        NeuralNetwork.recurrentTest(20,0.007,0.9,0.9,2,925,2,absolutePath);
        date = new Date();
        System.out.println(date);
    }
}