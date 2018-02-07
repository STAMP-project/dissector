package fr.inria.stamp.dissector.agent;

public class TargetMethod {

    private String container, name, desc;

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getFullName() {
        return container + "/" + name + desc;
    }


    //The line is not going to be used in equals or hashCode to avoid duplicated lines
    protected int line = -1;

    public TargetMethod(String container, String name, String desc) {
        this(container, name, desc, -1);
    }

    public TargetMethod(String container, String name, String desc, int line) {
        this.container = container;
        this.name = name;
        this.desc = desc;
        this.line = line;
    }

    @Override
    public int hashCode() {
        return container.hashCode() + 37 * (name.hashCode() + 37 * desc.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null) && obj instanceof TargetMethod && ((TargetMethod) obj).getFullName().equals(getFullName());
    }
}
