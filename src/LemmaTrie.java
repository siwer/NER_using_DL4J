import java.util.ArrayList;
import java.util.List;

//Die Datenstruktur für den Lemmatizer
//LemmaNode ist in LemmaTrie eingenistet
public class LemmaTrie implements java.io.Serializable{
    private LemmaNode root;

    public LemmaTrie () {
        this.root = new LemmaNode();
    }

    //Fügt ein Wort mit Index ein
    public void insert (String word,int index) {
        LemmaNode current = root;
        for (int i = 0;i<word.length();i++) {
            LemmaNode child = current.insert(word.charAt(i));
            current = child;
        }
        current.setEndofword(index);
    }

    //Liefert für ein vorhandenes Wort den Index zurück; ansonsten -1
    public int has(String word) {
        LemmaNode current = root;
        for (int i = 0;i<word.length();i++) {
            if (current.has(word.charAt(i))) {
                current = current.get(word.charAt(i));
            } else {
                return -1;
            }
        }
        return current.getEndofword();
    }

    class LemmaNode implements java.io.Serializable {
        private char data;
        private List<LemmaNode> children;
        private int endofword = -1;

        protected LemmaNode (char data) {
            this.data = data;
            this.children = new ArrayList<>();
        }

        protected LemmaNode () {
            this.children = new ArrayList<>();
        }

        protected LemmaNode insert(char data) {
            if (has(data)) {
                return get(data);

            }
            LemmaNode child = new LemmaNode(data);
            this.children.add(child);
            return child;
        }

        protected boolean has(char data) {
            for (LemmaNode child : this.children) {
                if (child.data == data) {
                    return true;
                }
            }
            return false;
        }

        protected LemmaNode get(char data) {
            for (LemmaNode child : this.children) {
                if (child.data == data) {
                    return child;
                }
            }
            return null;
        }

        protected void setEndofword (int index) {
            this.endofword = index;
        }

        protected int getEndofword () {
            return this.endofword;
        }


    }

}


