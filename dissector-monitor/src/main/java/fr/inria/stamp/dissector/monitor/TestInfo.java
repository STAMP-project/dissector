package fr.inria.stamp.dissector.monitor;

public class TestInfo {

    public String name;

    public boolean isAnnotated;


    public TestInfo(String name, boolean isAnnotated) {
        this.name = name;
        this.isAnnotated = isAnnotated;
    }

    @Override
    public boolean equals(Object obj) {
        if(! (obj instanceof  TestInfo) ) return false;
        TestInfo other = (TestInfo)obj;
        return other.isAnnotated && isAnnotated && other.name.equals(name);
    }

    @Override
    public int hashCode() {
        return  37 * name.hashCode() + (isAnnotated? 13 : 0);
    }

    @Override
    public String toString() {
        return (isAnnotated? "@org/junit/Test " : "") + name;
    }
}
