import java.io.Serializable;

public class SomeObject implements Serializable {
    private String id;

    public SomeObject() {
    }

    public SomeObject(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "SomeObject{" +
                "id='" + id + '\'' +
                '}';
    }
}
