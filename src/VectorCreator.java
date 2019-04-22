import java.util.ArrayList;
import java.util.List;

import static java.lang.Character.*;

public class VectorCreator {

    //Gibt eine ArrayList mit den ShapeFeatures zurück
    public static List<String> identityVecs (List<String> words) {
        List<String> out = new ArrayList<>();
        for (String word : words) {
            out.add(getVecForOneWord(word));
        }
       return out;
    }

    //Gibt für ein Wort die jeweiligen Features zurück
    public static String getVecForOneWord (String word) {
        return onlyNumbers(word) + "," + containsNumbers(word) + "," + onlyUpper(word) + "," + containsUpper(word) + "," + firstUpper(word) + "," + containsSpecial(word) + "," + containsNoLetters(word);
    }

    //Enthält ein Token ausschließlich Ziffern
    private static int onlyNumbers(String word) {
        int compare = word.length();
        int nrNrs = 0;
        for (int i = 0; i < compare; i++) {
            if (isDigit(word.codePointAt(i))) {
                nrNrs++;
            }
        }
        if (compare == nrNrs) return 1;
        else return 0;
    }

    //Enthält ein Token Ziffern
    private static int containsNumbers (String word) {
        for (int i = 0; i< word.length(); i++) {
            if (isDigit(word.codePointAt(i))) {
                return 1;
            }
        }
        return 0;
    }

    //Enthält ein Token ausschließlich Großbuchstaben
    private static int onlyUpper (String word) {
        int compare = word.length();
        int nrUp = 0;
        for (int i = 0; i < compare; i++) {
            if (isUpperCase(word.codePointAt(i))) {
                nrUp++;
            }
        }
        if (compare == nrUp) return 1;
        else return 0;
    }

    //Enthält ein Token Großbuchstaben
    private static int containsUpper (String word) {
        //0 wird ausgelassen, da dies in firstUpper getestet wird
        for (int i = 1; i< word.length(); i++) {
            if (isUpperCase(word.codePointAt(i))) {
                return 1;
            }
        }
        return 0;
    }

    //Ist der erste Zeichen des Tokens ein Großbuchstabe
    private static int firstUpper (String word) {
        if (isUpperCase(word.codePointAt(0))){
            return 1;
        }
        return 0;
    }

    //Enthält das Token Sonderzeichen
    private static int containsSpecial (String word) {
        for (int i = 0; i< word.length(); i++) {
            if (!isLetterOrDigit(word.codePointAt(i))) {
                return 1;
            }
        }
        return 0;
    }

    //Enthält das Token keine alphabetischen Zeichen
    private static int containsNoLetters (String word) {
        for (int i = 0; i< word.length(); i++) {
            if (!isLetter(word.codePointAt(i))) {
                return 1;
            }
        }
        return 0;
    }
}