package fr.inria.stamp.dissector.monitor;


import javassist.*;
import javassist.bytecode.MethodInfo;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

public class PublicMethodCollector {

    private ClassPool pool;
    private String root;
    private Set<String> collectedMethods;

    private static final String CLASS_EXT = ".class";

    public PublicMethodCollector(String root) throws IllegalArgumentException {
        collectedMethods = new HashSet<>();
        pool = ClassPool.getDefault();
        this.root = root;

        try {
            pool.appendClassPath(root);
        }
        catch (NotFoundException exc) {
            throw new IllegalArgumentException("Could not create a class pool from the given directory");
        }

    }

    public Set<String> collect() {

        collectedMethods.clear();

        try {
            Files.walkFileTree(Paths.get(root), new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String path = file.toString();
                    if (path.endsWith(CLASS_EXT))
                        collectFromFile(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException exc) {
            // Pass
        }

        return collectedMethods;
    }

    protected String getClassName(String path) {
        return path.substring(root.length() + 1, path.length() - CLASS_EXT.length()).replace('/', '.');
    }

    protected void collectFromFile(String path) {
        CtClass theClass = pool.getOrNull(getClassName(path));
        if(theClass == null || !Modifier.isPublic(theClass.getModifiers()))
            return;
        for (CtBehavior behavior : theClass.getDeclaredBehaviors()) {

            MethodInfo info = behavior.getMethodInfo();
            if(!Modifier.isPublic(behavior.getModifiers()) || (info.isConstructor() && behavior.isEmpty()))
                continue; // Not counting private methods or default constructors

            collectedMethods.add(theClass.getName().replace('.', '/') + "/" + info.getName() + info.getDescriptor());
        }
    }

    protected Set<String> collectFromClass(String className) {
        return null;
    }

}


