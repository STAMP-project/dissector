package eu.stamp_project.dissector.agent;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLogger {

    private BufferedWriter writer;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSZ");

    public FileLogger(File file) throws IOException {
        writer = new BufferedWriter(new FileWriter(file));
    }

    public void log(String line) {
        if(writer == null) return; //Nothing to say
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        }
        catch (IOException exc) {
            //Same here, just silence
        }
    }

    public void logWithTime(String message) {
        log(dateFormat.format(new Date(System.currentTimeMillis())), message);
    }

    public void log(String label, String message, Object... args) {
        log(String.format("[%S] %s", label, String.format(message, args)));
    }


}
