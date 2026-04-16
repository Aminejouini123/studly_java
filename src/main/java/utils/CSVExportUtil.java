package utils;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVExportUtil {

    public static <T> void exportToCSV(List<T> data, String[] headers, RowMapper<T> mapper, String defaultFileName, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Data to CSV");
        fileChooser.setInitialFileName(defaultFileName + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Write Header
                for (int i = 0; i < headers.length; i++) {
                    writer.append(escapeSpecialCharacters(headers[i]));
                    if (i < headers.length - 1) writer.append(",");
                }
                writer.append("\n");

                // Write Data
                for (T item : data) {
                    String[] row = mapper.map(item);
                    for (int i = 0; i < row.length; i++) {
                        writer.append(escapeSpecialCharacters(row[i]));
                        if (i < row.length - 1) writer.append(",");
                    }
                    writer.append("\n");
                }
                
                System.out.println("Export successful: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String escapeSpecialCharacters(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        String[] map(T item);
    }
}
