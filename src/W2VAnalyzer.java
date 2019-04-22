import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.Character.isLetterOrDigit;

public class W2VAnalyzer {

    //Entfernt übriggebliebene Zeichen
    private static String cleanWord (String word, boolean downcase) {
        if (downcase) {
            word = word.replaceAll("\\d", "di");
        }
        word = word.replaceAll("(<br>|<li>|<ul>|href=|</br>|</li>|</ul>|<span>|</span>|http|https)","");
        word = word.replaceAll("-(-+)","");
        word = word.replaceAll("\\.(\\.+)","");
        return word.replaceAll("(;|!|[=]|[?]|[+]|#|@|_|\"|[)]|[(]|'|`|/|…|“|[|]|–|©|\\[|\\]|\\{|}|\uD83D\uDE02|<|>|&|\\|»|«|„|”)","");
    }

    //Gibt einen leeren String zurück, sofern das Word word nur aus Sonderzeichen besteht
    private static String onlySpecial (String word) {
        int specialCount = 0;
        for (int i= 0;i<word.length();i++) {
            if (!isLetterOrDigit(word.codePointAt(i))) {
                specialCount++;
            }
        }
        if (specialCount == word.length()) {
            return "";
        } else {
            return word;
        }
    }

    //Sucht nach Substrings, welche im Lexikon vorkommen
    private static String decompose (String word, Word2Vec model,Lemmatizer lemma) {
        //Beginn von Vorne, damit nicht lexikalische Suffixe als Wort aufgefasst werden
        String incoming = word;
        if (word.length()>1) {
            for (int i = 1; i<word.length();i++) {
                String check = lemma.lemmatize(word.substring(i,word.length()-1));
                if (model.getWordVector(check)!=null) {
                    return check;
                }
            }
        }
        return incoming;
    }

    //Diente ursprünglich der Suche nach OOV Wörtern, wurde aber umgekehrt und gibt schließlich einen Vorverarbeitungsschritt des Inputs wieder
    public static void getOOVWords (String wordlistPath, Word2Vec model, Lemmatizer lemma, String output, boolean downcase) throws IOException {
        int decomposeCount = 0;
        Preprocessor reader = new Preprocessor();
        List<List<String>> data = reader.readCSVFile(wordlistPath);
        Writer writer =  new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Training_Ressources/"+ output +".csv"), "utf-8"));
        for (List<String> entry : data) {
            String currentWord;
            //Entfernen von Twitter URLs und allgemeine Stringbereinigung
            if (entry.get(0).matches("[A-z0-9]{10}")) {
                if (entry.get(0).matches("[A-z][a-z]{9}")) {
                    if (downcase) {
                        currentWord = lemma.lemmatize(cleanWord(entry.get(0),downcase).toLowerCase());
                    } else {
                        currentWord = lemma.lemmatize(cleanWord(entry.get(0),downcase));
                    }

                } else {
                    currentWord = "";
                }
            } else {
                if (downcase) {
                    currentWord = lemma.lemmatize(cleanWord(entry.get(0),downcase).toLowerCase());
                } else {
                    currentWord = lemma.lemmatize(cleanWord(entry.get(0),downcase));
                }

            }
            for (int i=0;i<3;i++) {
                if (currentWord.startsWith("»")||currentWord.startsWith("«")) {
                    currentWord = currentWord.substring(1);
                }
                if (currentWord.endsWith("«")||currentWord.endsWith("»")||currentWord.endsWith(":")||currentWord.endsWith(",")||currentWord.endsWith(".")||currentWord.endsWith("-")) {
                    currentWord = currentWord.substring(0,currentWord.length()-1);
                }
            }

            currentWord = onlySpecial(currentWord);
            //split composites with - ... natürlich nur wenn das Wort unbekannt ist
            int splitCompound = 0;
            int searchInCompound = 0;
            if (model.getWordVector(currentWord) == null) {
                if (currentWord.contains("-")) {
                    splitCompound = 1;
                    String [] tmp = currentWord.split("-");
                    for (int i = tmp.length - 1; i > 0; i--) {
                        String check = tmp[i];
                        //Überpüfung auf Kombination ORT-TEIL -> In dem Fall ist der Ort von Interesse
                        if (check.equals("nord") || check.equals("süd") || check.equals("ost") || check.equals("west") || check.equals("mitte") || model.getWordVector(lemma.lemmatize(check)) == null) {
                            currentWord = tmp[i - 1];
                            //Ansonsten ist der zweite Wortteil der allgemeinere und der erste Wortteil lediglich die Spezifizierung
                        } else {
                            currentWord = lemma.lemmatize(tmp[i]);
                        }
                        if (model.getWordVector(lemma.lemmatize(currentWord)) != null) {
                            break;
                        }
                    }
                    //handling of other composite words
                } else {
                    currentWord = decompose(currentWord,model,lemma);
                    searchInCompound = 1;
                    decomposeCount++;
                }
            }
            //discard empty strings
            if(currentWord.length()>1) {
                if (model.getWordVector(currentWord) != null) {
                    writer.write("'" + entry.get(2) + "';'" + entry.get(1) + "';'" + currentWord + "';'" + entry.get(0) + "';'" + splitCompound + "';'" + searchInCompound + "'" + "\n");
                    //writer.write( currentWord + "\n");
                }
            }
        }
        //System.out.println(decomposeCount);
        writer.close();
    }

    //Gibt das Wort zurück, welches der Summe der Wortvektoren am nächsten ist
    private static void getNearestVectorOfSum (String [] constituents, Word2Vec model) {
        INDArray a = model.getWordVectorMatrix(constituents[0]);
        for (int i = 1; i<constituents.length;i++) {
            INDArray b = model.getWordVectorMatrix(constituents[i]);
            a.add(b);
        }
        System.out.println(Arrays.toString(model.wordsNearestSum(a,5).toArray()));
    }

    //Gibt jeweils die nächsten drei Wörter zur Summe der Konstituentenvektoren an
    public static void printCompoundSums (String pfad) {
        Word2Vec model = WordVectorSerializer.readWord2VecModel("Training_Ressources/W2VModels/" + pfad);
        String [] test1 = new String[2];
        test1[0] = "europa";
        test1[1] = "tochter";
        System.out.println("Europa-Tocher:");
        getNearestVectorOfSum(test1,model);
        System.out.println("FIFA-Chef:");
        test1[0] = "fifa";
        test1[1] = "chef";
        getNearestVectorOfSum(test1,model);
        System.out.println("Rastatt-Süd:");
        test1[0] = "rastatt";
        test1[1] = "süd";
        getNearestVectorOfSum(test1,model);
        System.out.println("beinahe-Insolvenz:");
        test1[0] = "beinahe";
        test1[1] = "insolvenz";
        getNearestVectorOfSum(test1,model);
        System.out.println("Sattelzug-Fahrer:");
        test1[0] = "sattelzug";
        test1[1] = "fahrer";
        getNearestVectorOfSum(test1,model);
        System.out.println("Diebold-Aktie:");
        test1[0] = "diebold";
        test1[1] = "aktie";
        getNearestVectorOfSum(test1,model);
        System.out.println("Italien-Urlauber:");
        test1[0] = "italien";
        test1[1] = "urlauber";
        getNearestVectorOfSum(test1,model);
        System.out.println("Türkei-Europa-Gipfel:");
        String[] test2 = new String[3];
        test2[0] = "türkei";
        test2[1] = "eu";
        test2[2] = "gipfel";
        getNearestVectorOfSum(test2,model);
        System.out.println("Al-Salam-Krankenhaus:");
        test2[0] = "al";
        test2[1] = "salam";
        test2[2] = "krankenhaus";
        getNearestVectorOfSum(test2,model);
        System.out.println("Al-Kaida-Ableger:");
        test2[0] = "al";
        test2[1] = "kaida";
        test2[2] = "ableger";
        getNearestVectorOfSum(test2,model);
    }

    //Als Pfad wird das jeweilige Modell angegeben, welches sich im Ordner "Training_Ressources/W2VModels/" befinden
    public static void printTests (String pfad) {
        //Laden des Modells
        Word2Vec model = WordVectorSerializer.readWord2VecModel("Training_Ressources/W2VModels/" + pfad);
        List<String> testset = new ArrayList<>();
        //Aufbau des Testsets für ähnliche Wörter
        testset.add("geld");testset.add("bmw");testset.add("beethoven");testset.add("tolstoi");testset.add("idiot");testset.add("politik");
        testset.add("spd");testset.add("merkel");testset.add("saarland");testset.add("mannheim");testset.add("paris");testset.add("programmieren");
        testset.add("saufen");testset.add("klein");testset.add("schön");testset.add("sagen");testset.add("von");testset.add("der");
        testset.add("pferd");testset.add("schlagen");testset.add("deutschland");testset.add("montag");testset.add("nacht");testset.add("juli");
        testset.add("krieg");testset.add("könig");testset.add("paris");testset.add("europa");
        //Ausgabe des Testsets
        W2V.checkWordvectors(model,testset);
        //Testset für Arithmetik; die korrespondierenden Gleichungen sind angegeben
        System.out.println("König - Königin + Frau:");
        System.out.println(model.wordsNearest(Arrays.asList("könig","frau"),Arrays.asList("königin"),10));
        System.out.println("Washington - USA + Deutschland:");
        System.out.println(model.wordsNearest(Arrays.asList("washington","deutschland"),Arrays.asList("usa"),10));
        System.out.println("USA - Obama + Putin:");
        System.out.println(model.wordsNearest(Arrays.asList("usa","putin"),Arrays.asList("obama"),10));
        System.out.println("Merkel - CDU + Seehofer:");
        System.out.println(model.wordsNearest(Arrays.asList("merkel","seehofer"),Arrays.asList("cdu"),10));
        System.out.println("Flughafen - Flugzeug + Zug:");
        System.out.println(model.wordsNearest(Arrays.asList("flughafen","zug"),Arrays.asList("flugzeug"),10));
        System.out.println("Arm - Hand + Fuß:");
        System.out.println(model.wordsNearest(Arrays.asList("arm","fuß"),Arrays.asList("hand"),10));
        System.out.println("Westen - Osten + Süden:");
        System.out.println(model.wordsNearest(Arrays.asList("westen","süden"),Arrays.asList("osten"),10));
        System.out.println("Deutschland - deutsch + russisch:");
        System.out.println(model.wordsNearest(Arrays.asList("deutschland","russisch"),Arrays.asList("deutsch"),10));
        System.out.println("Armee - Soldat + Studenten:");
        System.out.println(model.wordsNearest(Arrays.asList("armee","student"),Arrays.asList("soldat"),10));
        System.out.println("Saarbrücken - Saarland + Rheinland-Pfalz:");
        System.out.println(model.wordsNearest(Arrays.asList("saarbrücken","rheinland-pfalz"),Arrays.asList("saarland"),10));
    }

}
