import de.dfki.lt.spree.io.AvroUtils;
import de.dfki.lt.tap.ConceptMention;
import de.dfki.lt.tap.Document;
import org.apache.avro.file.DataFileReader;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Preprocessor {

    //Für Trainings- und Testset
    //Liest das Korpus ein und erstellt zwei CSV Dateien
    public static void corpusToCsv (String corpuspath, String outputpathNE,String outputpathText) throws IOException {
        File inputFile = new File(corpuspath);
        DataFileReader<Document> reader = AvroUtils.createReader(inputFile);
        int j = 0;
        int i = 0;
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpathNE), "utf-8"));
        Writer writerTwo = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpathText), "utf-8"));
        try {
            writer.write("'id'" + ";" + "'NE'" + ";" + "'Type'" + ";" + "'IndexStart'" + ";" + "'IndexStop'" + ";" + "'TextId'" + "\n");
            writerTwo.write("'TextId'" + ";" + "'Text'" + "\n");
            while (reader.hasNext()) {
                j++;
                Document doc = reader.next();
                //System.out.print(j + " " + doc.getText());
                writerTwo.write("'" + j + "'" + ";" + "'" + doc.getText().replace("\n"," ")
                        + "'" + "\n");
                for (ConceptMention c : doc.getConceptMentions()) {
                    //System.out.print(c.getType());
                    i++;
                    writer.write("'" + i + "'" + ";" + "'" + c.getNormalizedValue() + "'" + ";" + "'" + c.getType()
                            + "'" + ";" + "'" + c.getSpan().getStart() + "'" +
                            ";" + "'" + c.getSpan().getEnd() + "'" + ";" + "'" + j + "'" + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writer.close();
            writerTwo.close();
        }
    }

    //Zum Einlesen von CSV zu Arraylist
    public static List<List<String>> readCSVFile (String name) {
        List<List<String>> lines = new ArrayList<>();
        String line = "";
        try (BufferedReader br = new BufferedReader(new FileReader(name))) {
            while ((line = br.readLine()) != null) {
                String[] columns = line.split("';'");
                columns = clearLineFromTextQuotes(columns);
                lines.add(Arrays.asList(columns));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    //Zum Einlesen und Aufteilen der Trainings- und Testdaten für Rnn
    public static List<List<String>> readCSVFileCorrectedforSplit (String name) {
        List<List<String>> lines = new ArrayList<>();
        String line = "";
        try (BufferedReader br = new BufferedReader(new FileReader(name))) {
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");
                columns = clearLineFromTextQuotes(columns);
                lines.add(Arrays.asList(columns));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    //Entfernt die String Delimiter in den CSV Dateien
    public static String [] clearLineFromTextQuotes (String [] line) {
        String [] result = new String [line.length];
        for (int i = 0; i < line.length; i++) {
            result[i] = line[i].replace("\'", "");
        }

        return result;
    }

    //Erstellt die Inputdaten mit einem Token pro Zeile und folgender Spaltenaufteilung: Token, NE, TextId
    public static void createRawInputData (String pathToTexts, String pathToNE,String outputpath) throws IOException {
        //An Whitespace Tokenisieren und NE Informationen erhalten
        List<List<String>> ne = readCSVFile(pathToNE);
        List<List<String>> text = readCSVFile(pathToTexts);
        /*newNe ist eine Liste, die eine Liste von Listen enthält,
        sodass das oberste Element jeweils die Annotationen für einen Text enthält;
        die Größe der Liste entspricht ergo der Größe von text-1, da in Text noch die Überschriften vorhanden sind
        */
        List<List<List<String>>> newNe = new ArrayList<>();
        //Start bei 1 wegen Überschriften
        for (int i = 1; i < text.size(); i++) {
            List<List<String>> temp = new ArrayList<>();
            //Start bei 1 wegen Überschriften
            for (List<String> entry : ne.subList(1, ne.size())) {
                if (Integer.parseInt(entry.get(5)) == i) {
                    temp.add(entry);
                }
            }
            newNe.add(temp);
        }
        //Zuweisung ohne Überschrift, sodass text und neNew gleiche Größe haben
        text = text.subList(1, text.size());

        if (text.size() == newNe.size()) {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpath), "utf-8"));
            try {
                for (int i = 0; i < text.size(); i++) {
                    //Enthält den aktuellen Text
                    //Ersetzung von Punkten und Kommata, die auf Buchstaben folgen durch Punkt+Blank bzw. Komma+Blank
                    String test = text.get(i).get(1).replaceAll(" "," ").replaceAll("((?<![^\\p{L}])\\.)",". ").
                            replaceAll("((?<![^\\p{L}]),)",", ").replaceAll("((?<![^\\p{L}]):)",": ").
                            replaceAll("((?<![^\\p{L}])/)","/ ");
                    //Splitten an Leerzeichen, sowie an Kommata und Punkten, welche nicht auf Zahlen folgen.
                    // Die nicht Blank Delimiter verschwinden dabei, jedoch bleibt die Information des Indexes erhalten, da vorher ein Leerzeichen zusätzlich hinzugefügt wurde
                    String arr[] = test.split("((?<=\\s)|(?=\\s+)|(?<![^\\p{L}])\\.)|(?<![^\\p{L}]),|((?<![^\\p{L}]):|((?<![^\\p{L}])/))");
                    //Werden unten mit den Start und Endpunkten der NEs befüllt
                    List<Integer> starts = new ArrayList<>();
                    List<Integer> ends = new ArrayList<>();
                    //Wird mit den NE-Types gefüllt
                    List<String> nes = new ArrayList<>();
                    //Iteriert für aktuellen Text (über Tid erkennbar) über die NE Liste
                    for (int j = 0; j < newNe.get(i).size(); j++) {
                        //newNe.get(i).get(j).get(x) -> 0=id,1=NE,2=Type,3=Start,4=Stopp,5=tid
                        starts.add(Integer.parseInt(newNe.get(i).get(j).get(3)));
                        ends.add(Integer.parseInt(newNe.get(i).get(j).get(4)));
                        nes.add(newNe.get(i).get(j).get(2));
                    }
                    //Zum Nachsehen; prinzipiell müsste überprüft werden, ob die Längen von starts, ends und nes gleich sind
                    //Counter für die Start, Stopp und NE Listen
                    int k = 0;
                    //Aktuelle Position im Text
                    int position = 0;
                    //Iteration über Text (Tokenweise)
                    for (String part : arr) {
                        position += part.length();
                        if (starts.isEmpty()) {
                            if (!part.isBlank()) {
                               writer.write("'" + part.trim() + "'" + ";" + "'" + "--" + "';'" + i + "'" + "\n");
                            }
                        } else {
                            if (position > starts.get(k) && position <= ends.get(k)) {
                                if (!part.isBlank()) {
                                    writer.write("'" + part.trim() + "'" + ";" + "'" + nes.get(k) + "';'" + i + "'" + "\n");
                                }
                            } else {
                                if (!part.isBlank()) {
                                    writer.write("'" + part.trim() + "'" + ";" + "'" + "--" + "';'" + i + "'" + "\n");
                                }
                            }
                            if (position > ends.get(k) && k < ends.size() - 1) {
                                k++;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                writer.close();
            }
        }
    }

    //Falls eine Stoppwortliste bei der Erstellung eines W2V Modells verwendet werden soll
    public static List<String> readStopwords(String path) {
        List<String> stopwords = new ArrayList<>();
        String line = new String();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while ((line = br.readLine()) != null) {
                stopwords.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopwords;
    }

    //Speichert ein Lemmatizer Objekt
    public static void saveLemmatizer (Lemmatizer toSave, String filename) {
        try {
            FileOutputStream fos = new FileOutputStream("Dictionary/" + filename + ".ser");
            ObjectOutput oos = new ObjectOutputStream(fos);
            oos.writeObject(toSave);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Lädt ein Lemmatizer Objekt
    public static Lemmatizer loadLemmatizer (String path) throws IOException {
        Lemmatizer reload = new Lemmatizer();
        try {
            FileInputStream fis = new FileInputStream(path);
            ObjectInput ois = new ObjectInputStream(fis);
            reload = (Lemmatizer) ois.readObject();
            ois.close();
            fis.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return reload;
    }

    //Input hier sind die Test/TrainOutput.csv Dateien
    //Output enthält BIO Tags
    public static void alterTags (String inputpath,String outputpath) throws IOException {
        List<List<String>> in = readCSVFile(inputpath);
        //in[x][1] beinhaltet jeweils den Tag
        List<List<String>> out = new ArrayList<>();
        //Für erste Zeile
        if (!in.get(0).get(1).equals("--")) {
            List<String> first = new ArrayList<>();
            //Wort
            first.add(in.get(0).get(0));
            //Textindex steht später in Spalte Nr. 2
            first.add(in.get(0).get(2));
            //Tag
            first.add(in.get(0).get(1)+ "BEGIN");
            out.add(first);
        } else {
            List<String> first = new ArrayList<>();
            first.add(in.get(0).get(0));
            first.add(in.get(0).get(2));
            first.add("OUT");
            out.add(first);
        }
        for (int i = 1; i < in.size();i++) {
            List<String> temp = new ArrayList<>();
            if (in.get(i).get(1).equals("--")) {
                //Wenn -- dann OUT
                temp.add(in.get(i).get(0));
                temp.add(in.get(i).get(2));
                temp.add("OUT");
            } else if (!in.get(i).get(1).equals(in.get(i-1).get(1))) {
                //anders als Vorgänger
                temp.add(in.get(i).get(0));
                temp.add(in.get(i).get(2));
                temp.add(in.get(i).get(1) + "BEGIN");
            } else if (in.get(i).get(1).equals(in.get(i-1).get(1))) {
                //gleich Vorgänger (und implizit ungleich --)
                temp.add(in.get(i).get(0));
                temp.add(in.get(i).get(2));
                temp.add(in.get(i).get(1)+"IN");
            }
            out.add(temp);
        }
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpath), "utf-8"));
        try {
            for (List<String> entry : out) {
                //WORT;TEXTID;TAG
                writer.write("'" + entry.get(0) + "';'" + entry.get(1) + "';'" + entry.get(2) + "'" + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writer.close();
        }
    }

    //Downcase und Lemmatize
    //Alles in einer Datei; derzeit nicht splitbar
    //dataPath enthält BIO Dateien
    public static void createInputdata (String dataPath, String lemmaPath, String wordModelPath,int wordModelDim, String wordShapeModelPath, int wordShapeDim,
                                 String shortShapeModelPath, int shortShapeDim, String outputpath, boolean rnn, boolean downcaseLemma, boolean oneHot) throws IOException {
        //wird unten zum Lemmatisieren genutzt
        Lemmatizer lemma = loadLemmatizer(lemmaPath);
        //behinhaltet die Test/TrainBIO.csv Datei
        List<List<String>> in = readCSVFile(dataPath);
        //wird mit den Wörtern aus in gefüllt und dann der Funktion wordstoVectorList übergeben
        List<String> wordsForModel = new ArrayList<>();
        List<String> wordsForOwnProcedure = new ArrayList<>();

        W2V model = new W2V();

        if (downcaseLemma) {
            for (List<String> line : in) {
                wordsForOwnProcedure.add(line.get(0));
                wordsForModel.add(lemma.lemmatize(line.get(0).toLowerCase()).replaceAll("\\d", "di"));
            }
        } else {
            for (List<String> line : in) {
                wordsForOwnProcedure.add(line.get(0));
                wordsForModel.add(line.get(0));
            }
        }
        //Weitere Methode für Vektorüberführung implementieren (anhand eigener Kriterien)
        //Diese erzeugt für jedes Wort in words einen Vektor, welcher in einer Liste gespeichert wird
        //Wenn alles richtig läuft, sind die Listen gleich lang und können zusammengeführt werden
        if (oneHot) {
            List<String> vectorsOne = model.wordsToVectorlist(wordModelPath, wordsForModel, wordModelDim, true);
            List<String> vectorsTwo = VectorCreator.identityVecs(wordsForOwnProcedure);
            if (rnn) {
                if (vectorsOne.size() == in.size() && vectorsTwo.size() == in.size()) {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpath), "utf-8"));
                    for (int i = 0; i < in.size(); i++) {
                        if (vectorsOne.get(i) != "null") {
                            //An erste Stelle kommt die TextID
                            String line = in.get(i).get(2) + "," + (tagToNr(in.get(i).get(1)) + "," + vectorsOne.get(i) + "," + vectorsTwo.get(i) + "\n");
                            writer.write(line.replaceAll("(\\[|]|\")", ""));
                        }
                    }
                    writer.close();
                }
            } else {
                if (vectorsOne.size() == in.size() && vectorsTwo.size() == in.size()) {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpath), "utf-8"));
                    for (int i = 0; i < in.size(); i++) {
                        if (vectorsOne.get(i) != "null") {
                            //Keine TextId, da nicht gesplittet wird
                            String line = (tagToNr(in.get(i).get(1)) + "," + vectorsOne.get(i) + "," + vectorsTwo.get(i) + "\n");
                            writer.write(line.replaceAll("(\\[|]|\")", ""));
                        }
                    }
                    writer.close();
                }
            }
        } else {
            Wordshape vecReturner = new Wordshape();
            //Enthält WordVecs
            List<String> vectorsOne = model.wordsToVectorlist(wordModelPath, wordsForModel, wordModelDim, true);
            List<String> vectorsTwo = new ArrayList<>();
            List<String> vectorsThree = new ArrayList<>();
            //Enthält ShapeVecs
            for (String word : wordsForOwnProcedure) {
                word = vecReturner.wordToShape(word);
                vectorsTwo.add(word);
                vectorsThree.add(vecReturner.wordShapeToShortShape(word));
            }
            List<String> shapeVec = model.wordsToVectorlist(wordShapeModelPath,vectorsTwo,wordShapeDim,true);
            List<String> shortShapeVec = model.wordsToVectorlist(shortShapeModelPath,vectorsThree,shortShapeDim,true);
            if (rnn) {
                if (vectorsOne.size() == in.size() && vectorsTwo.size() == in.size() && vectorsThree.size() == in.size()) {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpath), "utf-8"));
                    for (int i = 0; i < in.size(); i++) {
                        if (vectorsOne.get(i) != "null") {
                            //An erste Stelle kommt die TextID
                            String line = in.get(i).get(2) + "," + (tagToNr(in.get(i).get(1)) + "," + vectorsOne.get(i) + "," + shapeVec.get(i) + "," + shortShapeVec.get(i) + "\n");
                            writer.write(line.replaceAll("(\\[|]|\")", ""));
                        }
                    }
                    writer.close();
                }
            } else {
                if (vectorsOne.size() == in.size() && vectorsTwo.size() == in.size() && vectorsThree.size() == in.size()) {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpath), "utf-8"));
                    for (int i = 0; i < in.size(); i++) {
                        if (vectorsOne.get(i) != "null") {
                            //Keine TextId, da nicht gesplittet wird
                            String line = (tagToNr(in.get(i).get(1)) + "," + vectorsOne.get(i) + "," + shapeVec.get(i) + "," + shortShapeVec.get(i) + "\n");
                            writer.write(line.replaceAll("(\\[|]|\")", ""));
                        }
                    }
                    writer.close();
                }
            }
        }
    }

    //Wandelt die Tags in Zahlen um; BIO Tags sind derzeit auskommentiert
    public static String tagToNr (String tag) {
        HashMap<String,String> transformationTable = new HashMap(33);
        transformationTable.put("--","0");
        //transformationTable.put("organizationBEGIN","1");
        transformationTable.put("organization","1");
        //transformationTable.put("organization-companyBEGIN","3");
        transformationTable.put("organization-company","2");
        //transformationTable.put("personBEGIN","5");
        transformationTable.put("person","3");
        //transformationTable.put("locationBEGIN","7");
        transformationTable.put("location","4");
        //transformationTable.put("location-cityBEGIN","9");
        transformationTable.put("location-city","5");
        //transformationTable.put("location-streetBEGIN","11");
        transformationTable.put("location-street","6");
        //transformationTable.put("location-routeBEGIN","13");
        transformationTable.put("location-route","7");
        //transformationTable.put("location-stopBEGIN","15");
        transformationTable.put("location-stop","8");
        //transformationTable.put("org-positionBEGIN","17");
        transformationTable.put("org-position","9");
        //transformationTable.put("disaster-typeBEGIN","19");
        transformationTable.put("disaster-type","10");
        //transformationTable.put("triggerBEGIN","21");
        transformationTable.put("trigger","11");
        //transformationTable.put("dateBEGIN","23");
        transformationTable.put("date","12");
        //transformationTable.put("timeBEGIN","25");
        transformationTable.put("time","13");
        //transformationTable.put("durationBEGIN","27");
        transformationTable.put("duration","14");
        //transformationTable.put("numberBEGIN","29");
        transformationTable.put("number","15");
        //transformationTable.put("distanceBEGIN","31");
        transformationTable.put("distance","16");
        return transformationTable.get(tag);
    }

    //Test- & TrainOutput.csv sollen in Zeitreihen (also einzelne Texte) aufgespalten werden
    //Anschließend müssen diese Datein noch neu getaggt und die Tokens durch Vektoren ersetzt werden
    //Bekmommt das Ergebnis von createInputData mit TextID in erster Spalte
    public static void splitData (String dataPath, String outputpath) throws IOException {
        List<List<String>> dataToSplit = readCSVFile(dataPath);
        //INPUT: finalTest/Train.csv
        //Hat folgendes Format: word;ne;processedWord;TextNr;decompose;substring
        int MaxNr = Integer.parseInt(dataToSplit.get(dataToSplit.size()-1).get(0));
        int i = 0;
        //Init writer in Ordner outputpath mit Dateinamen i.csv
        Writer writerDATA = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpath + "DATA" + i + ".csv"), "utf-8"));
        Writer writerLABEL = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpath + "LABEL" + i + ".csv"), "utf-8"));
        //Iteration über den gesamten Datensatz
        for (int j = 0; j < dataToSplit.size();j++) {
            //Wenn Textnummer des aktuellen Eintrages dem Zähler entspricht, soll die Zeile in die aktuelle Datei geschrieben werden
            if (Integer.parseInt(dataToSplit.get(j).get(0)) == i) {
                //schreibt word;wordProcessed;decompose;substring
                writerDATA.write(("'" + dataToSplit.get(j).get(2) + "';'" + dataToSplit.get(j).get(3) + "';'" + dataToSplit.get(j).get(4) + "';'"  + dataToSplit.get(j).get(5) + "'" +"\n"));
                //schreibt neType
                writerLABEL.write((tagToNr(dataToSplit.get(j).get(1))).replaceAll("(\\[|])","") + "\n");
            }
            //Wenn Textnummer nicht dem Zähler entspricht, wird der Stream zur aktuellen Datei geschlossen und der Zähler inkrementiert
            if (Integer.parseInt(dataToSplit.get(j).get(0)) != i && i<MaxNr) {
                writerDATA.close();
                writerLABEL.close();
                i++;
                //Falls eine oder mehrer Zahlen übersprungen wurden, wird der Zähler weiter inkrementiert
                while (Integer.parseInt(dataToSplit.get(j).get(0)) != i) {
                    i++;
                }
                //Writer wird neu initialisiert mit Stream zur nächsten Datei
                writerDATA = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpath + "DATA" + i + ".csv"), "utf-8"));
                writerLABEL = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputpath + "LABEL" + i + ".csv"), "utf-8"));
                //Aktueller Eintrag wird in Datei geschrieben
                writerDATA.write(("'" + dataToSplit.get(j).get(2) + "';'" + dataToSplit.get(j).get(3) + "';'" + dataToSplit.get(j).get(4) + "';'"  + dataToSplit.get(j).get(5) + "'" +"\n"));
                writerLABEL.write((tagToNr(dataToSplit.get(j).get(1))).replaceAll("(\\[|])","") + "\n");
            }
        }
        //Am Ende muss der letzte writer geschlossen werden
        writerDATA.close();
        writerLABEL.close();
    }
}
