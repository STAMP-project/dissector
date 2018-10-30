package eu.stamp_project.dissector.monitor;

import com.google.gson.Gson;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;

public interface JsonReporterMojo<TReport> {

    File getOutput();

    TReport buildReport();

    default void saveReport(TReport report) throws MojoExecutionException {
        try {

            Gson gson = new Gson();
            try (FileWriter writer = new FileWriter(getOutput())) {
                gson.toJson(report, writer);
            }
        }
        catch (Throwable exc) {
            throw new MojoExecutionException("An error occurred while saving the report", exc);
        }
    }

    default void buildAndSaveReport() throws MojoExecutionException {
        saveReport(buildReport());
    }

}
