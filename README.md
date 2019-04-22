In diesem Repositorium befindet sich der Quellcode für das in der Masterarbeit beschrieben Projekt zur Named Entity Recognition.
Folgende externen Daten bzw. Skripte wurden dabei verwendet:
 - https://github.com/michmech/lemmatization-lists
 - https://dfki-lt-re-group.bitbucket.io/smartdata-corpus/
 - Wikipedia Dump:
   - https://dumps.wikimedia.org/dewiki/latest/dewiki-latest-pages-articles.xml.bz2
   - https://dumps.wikimedia.org/dewiki/latest/
 - Wikipedia Extractor (Website und Repo):
   - http://medialab.di.unipi.it/wiki/Wikipedia_Extractor
   - https://github.com/attardi/wikiextractor/blob/master/WikiExtractor.py
 - Skript zum Aufräumen des Wikipedia Extrakts:
   - https://github.com/devmount/GermanWordEmbeddings/blob/master/preprocessing.py

Die enthaltenen Klassen und ihre Funktionalität sollen hier kurz beschrieben werden:
 - Klassen, welche Interfaces von DL4J implementieren (Diese dienen als Vorverarbeitungseinheiten beim Training von W2V-Modellen):
   - CaseIgnorantPreProcessor (gibt das Token komplett unverändert zurück)
   - LowCaseLemmaPreProcessor (gibt das Token lemmatisiert, Downcase und von Sonderzeichen befreit zurück)
   - CustomTokenizer (splittet die aktuelle Zeile anhand von regulären Ausdrücken in Tokens; wird für das Wortmodell benutzt)
   - CustomTokenizerFactory (Factory Klasse für den CustomTokenizer)
   - ShapeTokenizer (splittet die aktuelle Zeile an einem oder mehreren Whitespacezeichen; wird für das Shapemodell benutzt)
   - ShapeTokenizerFactory (Factory Klasse für den ShapeTokenizer)
 - Preprocessor
   - stellt Funktionen zum Einlesen und Umwandeln des Korpus bereit, sowie verschiedene Hilfsfunktionen, die für weitere Umformungen benötigt werden
   - createInputdata und alterTags sind derzeit nicht in Verwendung
 - Klassen für den Lemmatizer
   - LemmaTrie (stellt die Datenstruktur bereit)
   - Lemmatizer (verfügt über Funktionen zum Aufbau des Tries und zum Lemmatisieren)
 - Klassen für die Erstellung der Finalen Daten
   - VectorComposer(überführt die Dateien aus der Vorverarbeitung zu Dateien, die die Vektorrepräsentationen der Features enthalten)
   - VectorCreator (beinhaltet Funktionen zum Generieren des sparse Wordshapevektors)
   - WordShape (Wandelt Wörter (Tokens) in ihre Wordshape um)
   - Datatransformer (Überführt die einzelnen Dateien für RNN in eine Datei für FF)
   - W2VAnalyzer (diente ursprünglich der OOV Analyse, verfügt nun über eine Funktion zum Erstellen von Daten); enthält Funktionen mit vorgefertigten Testsets für das W2V Modell
 - Klassen, welche DL4J Methoden implementieren (diese dienen dem Training der W2V Modelle und der neuronalen Netzwerke)
   - W2V (trainiert und speichert ein W2V Modell)
   - NeuralNetwork (trainiert und evaluiert verschiedene Neuronale Netzwerke)

Workflow:
 1. Download Korpus, Download Wikipedia Dump
 2. Run WikiExtractor.py, Run preprocessing.py
 - Klasse W2V
 3. Training der Modelle mit createVecRepresentation
 - Klasse Preprocessor
 4. corpusToCsv (mit Test- und Trainingsset)
 5. createRawInputData (Mit den Resultaten aus korpusToCsv)
 6. Laden des Lemmatizers und W2V Modells
 - Klasse W2VAnalyzer
 7. getOOVWords (Training- und Testset)
 - Klasse Preprocessor
 8. splitData
 9. Laden der Shape Modelle
 - Klasse VectorComposer
 10. transformAllFolder (man erhält die RNN Daten)
 - Klasse DataTransformer
 11. rnnDataToFF (man erhält die FF Daten)
 - Klasse NeuralNetwork
 12. feedForwardTest
 13. recurrentTest
