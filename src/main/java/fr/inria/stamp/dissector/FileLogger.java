package fr.inria.stamp.dissector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLogger {

    private BufferedWriter writer;
    SimpleDateFormat dateFormat = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss:SSSZ]");

    public FileLogger(Path path) {
        try {
            writer = Files.newBufferedWriter(path);
        }
        catch(IOException exc) {
            //Silence as we can't log an error in the logger.
            writer = null;
        }
    }

    public FileLogger(String path) {
        this(Paths.get(path));
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

    public void log(String... args) {
        for(String str: args) {
            log(str);
        }
    }

    public void logWithTime(String message) {
        log(dateFormat.format(new Date(System.currentTimeMillis())) + " " + message);
    }

    public void log(String message, Object... args) {
        log(String.format(message, args));
    }


}
