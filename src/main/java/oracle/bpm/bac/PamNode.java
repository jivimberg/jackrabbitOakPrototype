package oracle.bpm.bac;

import java.util.Calendar;

public abstract class PamNode {

    private final String id;
    private final Calendar creationDate;
    private final String creator;

    public PamNode(String id, Calendar creationDate, String creator) {
        this.id = id;
        this.creationDate = creationDate;
        this.creator = creator;
    }

    public String getId() {
        return id;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public String getCreator() {
        return creator;
    }

    @Override
    public String toString() {
        return "PamNode{" +
                "id='" + id + '\'' +
                ", creator='" + creator + '\'' +
                '}';
    }
}
