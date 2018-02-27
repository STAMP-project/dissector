package fr.inria.stamp.dissector.monitor;

import javassist.ClassPool;
import javassist.NotFoundException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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


    protected Set<TestInfo> getTestMethods() throws MojoExecutionException {

        String rootFolder = _project.getBuild().getTestOutputDirectory();
        ClassPool pool = ClassPool.getDefault();

        try {
            pool.appendClassPath(rootFolder);
        }
        catch (NotFoundException exc) {
            throw new MojoExecutionException("Unexpected error from javassist. Details: " + exc.getMessage(), exc);
        }

        TestMethodCollector collector;
        try {
            collector = new TestMethodCollector(pool);
        }
        catch (NotFoundException exc) {
            throw new MojoExecutionException("Unexpected error while collecting test methods. Details: " + exc.getMessage(), exc);
        }

        try {

            Files.walkFileTree(Paths.get(rootFolder), new FileVisitor<Path>() {

                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String filePath = file.toString();
                    String extension = ".class";
                    if (!filePath.endsWith(extension))
                        return FileVisitResult.CONTINUE; //Using this instead of Path's capabilities due to an odd behavior in Path endsWith
                    String className = filePath.substring(rootFolder.length() + 1, filePath.length() - extension.length()).replace('/', '.');
                    try {
                        collector.collectFrom(className);
                    } catch (NotFoundException exc) {
                        throw new IOException(exc);
                    }
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

            });
            return collector.getTestMethods();
        }
        catch (IOException exc) {
            throw new MojoExecutionException("An error occurred while inspecting test class files. Details: " + exc.getMessage(), exc);
        }

    }




}
