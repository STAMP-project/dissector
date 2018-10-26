package eu.stamp_project.dissector.monitor;

import org.apache.maven.plugin.MojoExecutionException;

public class CallGraphMojo extends DynamicDissectorMojo {

    @Override
    protected void prepareExecution() throws MojoExecutionException {

    }

    @Override
    protected void onMethodExit(int thread, int method, int depth) {

    }

    @Override
    protected void onMethodEnter(int thread, int method, int depth) {

    }

    @Override
    protected void finishExecution() throws MojoExecutionException {

    }

}
