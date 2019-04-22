import java.io.*;
import java.util.List;

//Fügt die einzelnen Dateien, welche jeweils einen Text beinhalten zu einer großen Datei zusammen (für feed-forward Training)
public class DataTransformer {
    public static void rnnDataToFF (String inputfolder,String inputFileShape,String labelFolder, String inputLabelShape,String output, int txtNr) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Training_Ressources/" + output + ".csv"), "utf-8"));
        List<List<String>> labels;
        List<List<String>> data;

        for (int i = 0;i<txtNr;i++) {
            data = Preprocessor.readCSVFileCorrectedforSplit(inputfolder + i + inputFileShape + ".csv");
            labels = Preprocessor.readCSVFileCorrectedforSplit(labelFolder + inputLabelShape +i + ".csv");
            if (data.size() == labels.size()) {
                for (int j = 0; j<data.size();j++) {
                    writer.write(labels.get(j).toString().replaceAll("(\\[|]|\")", "") + "," +data.get(j).toString().replaceAll("(\\[|]|\")", "") + "\n");
                }
            }
        }
        writer.close();
    }
}
