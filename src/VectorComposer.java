import org.deeplearning4j.models.word2vec.Word2Vec;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VectorComposer {

    // -> Training_Ressources/RnnData/Test/     writefolder
    // -> Training_Ressources/RnnDate/TestRead  readfolder
    public static void transformAllFolder (String readfolder, String writeFolder, Word2Vec model,int nrBehind,int nrAhead, boolean shape, Word2Vec shapeModel, Word2Vec shortShapeModel,
                                           boolean shapeDense, boolean shortDense,boolean similarity, int simAhead, int simBehind,boolean train) throws IOException {
        int max;
        if (train) {max = 1243;}
        else {max = 1191;}
        for (int i = 0;i<max;i++) {
            String path = readfolder + "DATA" + i + ".csv";
            composeFeatureVector(path,writeFolder,model,nrBehind,nrAhead,shape,shapeModel,shortShapeModel,shapeDense,shortDense,similarity,simAhead,simBehind,i);
        }
    }

    //Baut nach den Vorgaben der Parameter die Trainings und Testdateien, welche als Input für ein neuronales Netzwerk dienen zusammen.
    public static void composeFeatureVector (String wordlistPath, String outputPath ,Word2Vec wordModel,
                                      int nrBehind, int nrAhead, boolean shape, Word2Vec shapeModel,Word2Vec shortShapeModel,
                                      boolean shapeDense,boolean shortDense, boolean similarity, int simAhead,int simBehind, int number) throws IOException {
        Preprocessor reader = new Preprocessor();
        List<List<String>> data = reader.readCSVFile(wordlistPath);
        int wordDimension = wordModel.getLayerSize();
        //Wortidentität mit Embeddings
        int dim = 0;
        List<String> start = getWordRepresentationsFF(data,wordModel);
        //+2 entspricht den Informationen, ob ein Wort geändert wurde
        dim += wordDimension+2;
        //Init für andere Features
        List<String> wordBehind = new ArrayList<>();
        List<String> wordAhead = new ArrayList<>();
        List<String> wordShape = new ArrayList<>();
        List<String> wordShapeEmbedd = new ArrayList<>();
        List<String> shortShapeEmbedd = new ArrayList<>();
        List<String> wordShapeBehind = new ArrayList<>();
        List<String> wordShapeBefore = new ArrayList<>();
        List<String> similarityBehind = new ArrayList<>();
        List<String> similarityBefore = new ArrayList<>();
        //für jedes folgende Word im Bereich 1 bis nrBehind
        if (nrBehind>0) {
            List<List<String>> tmp = new ArrayList<>();
            for (int i = 1; i <= nrBehind; i++) {
                tmp.add(getWordsAfter(data, wordModel, i));
                dim += wordDimension;
            }
            //Für jede Iteration über das Datenset wird ein String initialisiert
            for (int i=0;i<tmp.get(0).size();i++) {
                String combine = "";
                //Für jeden Eintrag müssen alle Sublisten kombiniert werden
                for (int j=0;j<tmp.size();j++) {
                    combine += tmp.get(j).get(i);
                }
                wordBehind.add(combine);
            }
        }
        //für jedes vorangehende Wort im Bereich 1 bis nrAhead
        if (nrAhead>0) {
            List<List<String>> tmp = new ArrayList<>();
            for (int i = 1; i <= nrAhead; i++) {
                tmp.add(getWordsBefore(data, wordModel, nrAhead));
                dim += wordDimension;
            }
            for (int i = 0; i < tmp.get(0).size(); i++) {
                String combine = "";
                for (int j = 0; j < tmp.size(); j++) {
                    combine += tmp.get(j).get(i);
                }
                wordAhead.add(combine);
            }
        }
        //aktuelle Wordshape
        if (shape) {
            wordShape = getWordShape(data);
            dim += 7;
            if (nrBehind>0) {
                List<List<String>> tmp = new ArrayList<>();
                for (int i = 1;i <= nrBehind;i++) {
                    tmp.add(getWordShapeAfter(data,nrBehind));
                    dim += 7;
                }
                for (int i = 0; i < tmp.get(0).size(); i++) {
                    String combine = "";
                    for (int j=0;j<tmp.size();j++) {
                        combine += tmp.get(j).get(i);
                    }
                    wordShapeBehind.add(combine);
                }
            }
            if (nrAhead>0) {
                List<List<String>> tmp = new ArrayList<>();
                for (int i=1;i<=nrAhead;i++) {
                    tmp.add(getWordShapeBefore(data,nrAhead));
                    dim += 7;
                }
                for (int i = 0;i <tmp.get(0).size();i++) {
                    String combine = "";
                    for (int j = 0;j<tmp.size();j++) {
                        combine += tmp.get(j).get(i);
                    }
                    wordShapeBefore.add(combine);
                }
            }
        }
        //Nachfolger + Vorgänger hinzufügen?
        if (shapeDense) {
            wordShapeEmbedd = getWordShapeEmbedd(data,shapeModel);
            dim += shapeModel.getLayerSize();
        }
        if (shortDense) {
            shortShapeEmbedd = getShortShapeEmbedd(data,shortShapeModel);
            dim += shapeModel.getLayerSize();
        }
        if (similarity) {
            if (simAhead>0) {
                List<List<String>> tmp = new ArrayList<>();
                for (int i = 1; i <= simAhead; i++) {
                    tmp.add(getSimilarityBefore(data, wordModel, simAhead));
                    dim += 1;
                }
                for (int i = 0; i < tmp.get(0).size(); i++) {
                    String combine = "";
                    for (int j = 0; j < tmp.size(); j++) {
                        combine += tmp.get(j).get(i);
                    }
                    similarityBefore.add(combine);
                }
            }
            if (simBehind>0) {
                List<List<String>> tmp = new ArrayList<>();
                for (int i = 1; i <= simBehind; i++) {
                    tmp.add(getSimilarityBehind(data, wordModel, simBehind));
                    dim += 1;
                }
                for (int i = 0; i < tmp.get(0).size(); i++) {
                    String combine = "";
                    for (int j = 0; j < tmp.size(); j++) {
                        combine += tmp.get(j).get(i);
                    }
                    similarityBehind.add(combine);
                }
            }
        }
        List<List<String>> result = new ArrayList<>();
        result.add(start);
        if (!wordBehind.isEmpty()) {result.add(wordBehind);}
        if (!wordAhead.isEmpty()) {result.add(wordAhead);}
        if (!wordShape.isEmpty()) {result.add(wordShape);}
        if (!wordShapeBehind.isEmpty()) {result.add(wordShapeBehind);}
        if (!wordShapeBefore.isEmpty()) {result.add(wordShapeBefore);}
        if (!wordShapeEmbedd.isEmpty()) {result.add(wordShapeEmbedd);}
        if (!shortShapeEmbedd.isEmpty()) {result.add(shortShapeEmbedd);}
        if (!similarityBefore.isEmpty()) {result.add(similarityBefore);}
        if (!similarityBehind.isEmpty()) {result.add(similarityBehind);}

        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + number + "Dim_" + dim +".csv"), "utf-8"));
        for (int i = 0 ; i<start.size();i++) {
            String combinedVectors = "";
            for (int j = 0; j<result.size();j++) {
                combinedVectors += result.get(j).get(i);
            }
            writer.write(combinedVectors + "\n");
        }
        writer.close();
    }

    //Entfernt Klammern, die bei Arrays.toString auftauchen
    private static String cleanEntry (String entry) {
        return entry.replaceAll("(\\[|]|\")", "");
    }


    //Hat folgendes Format: processedWord;Word;decompose;substring
    //Gibt die Liste von Vektorrepräsentationen fürs Training und Testen zurück
    private static List<String> getWordRepresentationsFF (List<List<String>> data, Word2Vec model) {
        List<String> output = new ArrayList<>();
        double[] zeros = new double[model.getLayerSize()];
        for (List<String> line : data) {
            if (model.getWordVector(line.get(0))==null) {
                output.add(cleanEntry(Arrays.toString(zeros)) + "," + line.get(2)+ "," + line.get(3));
            } else {
                //get(0) entspricht verarbeitetem Token
                output.add(cleanEntry(Arrays.toString(model.getWordVector(line.get(0))) + "," + line.get(2)+ "," + line.get(3)));
            }
        }
        return output;
    }

    private static List<String> getWordsBefore (List<List<String>> data, Word2Vec model, int nrBefore) {
        List<String> output = new ArrayList<>();
        double[] zeros = new double[model.getLayerSize()];
        //Es exisitieren keine Vorgänger für die ersten nrBefore Wörter
        for (int j = 0;j<nrBefore;j++) {
            output.add(cleanEntry("," + Arrays.toString(zeros)));
        }
        for (int i = nrBefore;i<data.size();i++) {
            if (model.getWordVector(data.get(i-nrBefore).get(0))==null) {
                output.add(cleanEntry("," + Arrays.toString(zeros)));
            } else {
                output.add(cleanEntry("," + Arrays.toString(model.getWordVector(data.get(i-nrBefore).get(0)))));
            }
        }
        return output;
    }

    private static List<String> getWordsAfter (List<List<String>> data, Word2Vec model, int nrAfter) {
        List<String> output = new ArrayList<>();
        double[] zeros = new double[model.getLayerSize()];

        for (int i = 0;i<data.size()-nrAfter;i++) {
            if (model.getWordVector(data.get(i+nrAfter).get(0))==null) {
                output.add(cleanEntry("," + Arrays.toString(zeros)));
            } else {
                output.add(cleanEntry("," + Arrays.toString(model.getWordVector(data.get(i+nrAfter).get(0)))));
            }
        }
        for (int j = 0;j<nrAfter;j++) {
            output.add(cleanEntry("," + Arrays.toString(zeros)));
        }
        return output;
    }

    private static List<String> getWordShape (List<List<String>> data) {
        List<String> output = new ArrayList<>();
        //Wordshape bekommt das nicht verarbeitete Wort
        for (List<String> entry : data) {
            output.add("," + VectorCreator.getVecForOneWord(entry.get(1)));
        }
        return output;
    }

    private static List<String> getWordShapeBefore (List<List<String>> data, int nrBefore) {
        VectorCreator shapefeature = new VectorCreator();
        List<String> output = new ArrayList<>();

        for (int j = 0; j<nrBefore; j++) {
            output.add(",0,0,0,0,0,0,0");
        }
        for (int i = nrBefore; i< data.size();i++) {
            output.add("," + shapefeature.getVecForOneWord(data.get(i-nrBefore).get(1)));
        }
        return output;
    }

    private static List<String> getWordShapeAfter (List<List<String>> data, int nrAfter) {
        VectorCreator shapefeature = new VectorCreator();
        List<String> output = new ArrayList<>();

        for (int i = 0; i<data.size()-nrAfter; i++) {
            output.add("," + shapefeature.getVecForOneWord(data.get(i+nrAfter).get(1)));
        }
        for (int j = 0;j<nrAfter;j++) {
            output.add(",0,0,0,0,0,0,0");
        }
        return output;
    }

    private static List<String> getWordShapeEmbedd (List<List<String>> data, Word2Vec shapeModel) {
        List<String> output = new ArrayList<>();
        double[] zeros = new double[shapeModel.getLayerSize()];
        Wordshape shape = new Wordshape();

        for (List<String> entry : data) {
            String word = shape.wordToShape(entry.get(1));
            String compose = ",";
            if (shapeModel.getWordVector(word)!=null) {
                compose += Arrays.toString(shapeModel.getWordVector(word));
            } else {
                compose += Arrays.toString(zeros);
            }
            output.add(cleanEntry(compose));
        }
        return output;
    }

    private static List<String> getShortShapeEmbedd (List<List<String>> data, Word2Vec shortShapeModel) {
        List<String> output = new ArrayList<>();
        double[] zeros = new double[shortShapeModel.getLayerSize()];
        Wordshape shape = new Wordshape();

        for (List<String> entry : data) {
            String word = shape.wordToShape(entry.get(1));
            word = shape.wordShapeToShortShape(word);
            String compose = ",";
            if (shortShapeModel.getWordVector(word)!=null) {
                compose += Arrays.toString(shortShapeModel.getWordVector(word));
            } else {
                compose += Arrays.toString(zeros);
            }
            output.add(cleanEntry(compose));
        }
        return output;
    }

    //Distanzen zu Vorgänger und Nachfolger
    private static List<String> getSimilarityBehind (List<List<String>> data, Word2Vec model, int nrAfter) {
        List<String> output = new ArrayList<>();
        for (int i = 0;i<data.size()-nrAfter;i++) {
            //akutell
            String wordA = data.get(i).get(0);
            //nachfolger
            String wordB = data.get(i+nrAfter).get(0);
            output.add(cleanEntry("," + model.similarity(wordA,wordB)));
        }
        for (int j = 0;j<nrAfter;j++) {
            output.add(cleanEntry(",0.0"));
        }
        return output;
    }

    private static List<String> getSimilarityBefore (List<List<String>> data, Word2Vec model, int nrBefore) {
        List<String> output = new ArrayList<>();

        for (int j = 0;j<nrBefore;j++) {
            output.add(cleanEntry(",0.0"));
        }
        for (int i = nrBefore;i<data.size();i++) {
            //aktuell
            String wordA = data.get(i).get(0);
            //vorgänger
            String wordB = data.get(i-nrBefore).get(0);
            output.add(cleanEntry("," + model.similarity(wordA,wordB)));
        }
        return output;
    }
}
