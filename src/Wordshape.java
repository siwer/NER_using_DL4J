import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Wordshape {

    //Überführt die Datei KorpusInputForW2V.txt in zwei Dateien -> wordshape und shortwordshape zum Trainieren von Embeddings
    public void transformAll (String inpath,String shapeOutpath,String shortShapeOutpath) throws IOException {
        //contains lines as Strings
        List<String> linesShape = new ArrayList<>();
        List<String> linesShortShape = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(inpath))) {
            while ((line = br.readLine()) != null) {
                line = wordToShape(line);
                linesShape.add(line);
                linesShortShape.add(wordShapeToShortShape(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(shapeOutpath), "utf-8"));
        for (String writeline : linesShape) {
            writer.write(cleanLine(writeline) + "\n");
        }
        writer.close();
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(shortShapeOutpath), "utf-8"));
        for (String writeline : linesShortShape) {
            writer.write(cleanLine(writeline) + "\n");
        }
        writer.close();
    }


    public String wordToShape (String line) {

        return line.replaceAll("\\p{javaLowerCase}","x").replaceAll("\\p{javaUpperCase}","X").replaceAll("\\d","d");
    }

    public String wordShapeToShortShape (String line) {
        List<Character> chars = new ArrayList<>();
        for (Character c : line.toCharArray()) {
            chars.add(c);
        }

        for (int i = 0; i<chars.size()-1;i++) {
            if (chars.get(i).equals(chars.get(i+1))) {
                chars.set(i,null);
            }
        }
        //chars.removeAll(null);
        return chars.toString().replaceAll("(, |null|\\[|])","");
    }

    private String cleanLine (String line) {
        return line.replaceAll("(;|!|\\?|\\+|#|@|_|\"|[)]|[(]|'|`|/|…|“|»|«|–|©|\\[|]|\\{|}|\uD83D\uDE02|<|>|&|\\|)","").replaceAll("(?<=x)[.]|(?<=X)[.]|(?<=x)[,]|(?<=X)[,]","");
    }

    public List<String> wordShapeFeatureVector (String modelpath, int dimensions, List<String> words) {
        List<String> out = new ArrayList<>();
        Word2Vec vec = WordVectorSerializer.readWord2VecModel(modelpath);
        double[] oov = new double[dimensions];
        int oovCount = 0;
        for (String word : words) {
            if (vec.getWordVector(word) == null) {
                out.add(Arrays.toString(oov));
                oovCount++;
            } else {
                out.add(Arrays.toString(vec.getWordVector(wordToShape(word))));
            }
        }
        System.out.println("OOV Wordshape: " + oovCount);
        return out;
    }

    public List<String> shortShapeFeatureVector (String modelpath, int dimensions, List<String> words) {
        List<String> out = new ArrayList<>();
        Word2Vec vec = WordVectorSerializer.readWord2VecModel(modelpath);
        double[] oov = new double[dimensions];
        int oovCount = 0;
        for (String word : words) {
            if (vec.getWordVector(word) == null) {
                out.add(Arrays.toString(oov));
                oovCount++;
            } else {
                out.add(Arrays.toString(vec.getWordVector(wordShapeToShortShape(wordToShape(word)))));
            }
        }
        System.out.println("OOV Shortshape: " + oovCount);
        return out;
    }

}
