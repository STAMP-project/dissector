package eu.stamp_project.dissector.monitor;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Set;


public abstract class DissectorMojo extends AbstractMojo {

    @Parameter(defaultValue="${project}")
    protected MavenProject _project;

    public MavenProject getProject() {
        return _project;
    }

    public void setProject(MavenProject project) {
        _project = project;
    }


    protected boolean hasValue(String parameter) {
        return parameter != null && !parameter.equals("");
    }


}
