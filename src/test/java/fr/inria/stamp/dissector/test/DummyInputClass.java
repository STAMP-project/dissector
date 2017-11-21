package fr.inria.stamp.dissector.test;

//TODO: Find a way to execute this as a unit test

//To try:
//mvn clean package
//java -javaagent:target/runtime-inspector-1.0-SNAPSHOT.jar -cp /Users/overaper/.m2/repository/org/javassist/javassist/3.21.0-GA/javassist-3.21.0-GA.jar:src/test/java/ fr.inria.stamp.inspector.test.DummyInputClass

//java -javaagent:target/runtime-inspector-1.0-SNAPSHOT.jar -cp /Users/overaper/.m2/repository/fr/inria/stamp/inspector-database:/Users/overaper/.m2/repository/org/javassist/javassist/3.21.0-GA/javassist-3.21.0-GA.jar:src/test/java/ fr.inria.stamp.inspector.test.DummyInputClass



//java -javaagent:target/runtime-inspector-1.0-SNAPSHOT.jar -cp target/runtime-inspector-1.0-SNAPSHOT.jar:/Users/overaper/.m2/repository/org/javassist/javassist/3.21.0-GA/javassist-3.21.0-GA.jar:src/test/java/ fr.inria.stamp.inspector.test.DummyInputClass

public class DummyInputClass {

    public static void main(String[] args) {
        DummyInputClass dummy = new DummyInputClass();
        dummy.sayHi();
        System.out.println(dummy.getAge());
        dummy.sayHiTo("Someone");
    }

    public void sayHi() { System.out.println("Hi"); }

    public void sayHiTo(String name) { System.out.println("Hi " + name); }

    public int getAge() { return 1; }

}
