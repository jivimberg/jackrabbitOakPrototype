package oracle.bpm.bac;

import java.util.Calendar;

public class Project extends PamNode {

    private final String description;
    private final String spaceId;

    public Project(String spaceId, String projectId, Calendar creationDate, String creator, String description) {
        super(projectId, creationDate, creator);
        this.description = description;
        this.spaceId = spaceId;
    }

    public String getDescription() {
        return description;
    }

    public String getSpaceId() {
        return spaceId;
    }

    @Override
    public String toString() {
        return "Project{" +
                "description='" + description + '\'' +
                ", spaceId='" + spaceId + '\'' +
                "} " + super.toString();
    }
}
