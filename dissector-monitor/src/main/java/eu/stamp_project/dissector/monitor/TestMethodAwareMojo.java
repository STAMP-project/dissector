package eu.stamp_project.dissector.monitor;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.Set;

public interface TestMethodAwareMojo {

    MavenProject getProject();

    Log getLog();

    default Set<String> getTestMethods() {
        MavenProject project = getProject();
        getLog().debug("Collecting test methods from: " + project.getBuild().getTestOutputDirectory());
        return new MethodCollector(project.getBuild().getTestOutputDirectory()).collect();
    }

}
