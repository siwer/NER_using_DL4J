import java.io.*;
import java.util.ArrayList;
import java.util.List;

//Die Klasse für den Lemmatizer
//Enthält Methoden zum Aufbau des Tries, der Wordlist und eine Methode die für ein gegebenes Wort das Lemma zurückgibt
public class Lemmatizer implements java.io.Serializable {
    //Beinaltet den Trie
    private LemmaTrie lexicon;
    //Beinhaltet die Lemmata an der Stelle, die dem Rückgabewert des Tries entspricht
    private List<String> wordlist;

    public Lemmatizer() {}

    public void buildLexicon (String indexedFilePath) {
        this.lexicon = new LemmaTrie();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(indexedFilePath))) {
            while ((line = br.readLine()) != null) {
                String [] tmp = line.split(" ");
                this.lexicon.insert(tmp[0],Integer.parseInt(tmp[2]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void buildWordlist (String indexedFilePath) {
        this.wordlist = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(indexedFilePath))) {
            while ((line = br.readLine()) != null) {
                String [] tmp = line.split(" ");
                if(!this.wordlist.contains(tmp[1])) {
                    this.wordlist.add(tmp[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String lemmatize(String word) {
        if (this.lexicon.has(word)!=-1) {
            return this.wordlist.get(this.lexicon.has(word));
        } else {
            return word;
        }
    }

}
