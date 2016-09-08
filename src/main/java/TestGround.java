import oracle.bpm.bac.PamServiceImpl;
import oracle.bpm.bac.Space;

import javax.jcr.RepositoryException;
import java.net.UnknownHostException;
import java.util.Collection;

public class TestGround {

    public static void main(String[] args) throws RepositoryException, UnknownHostException {
        PamServiceImpl bacService = new PamServiceImpl();

        String spaceName = "mySpace1";
//        bacService.createSpace(spaceName);
//        bacService.createProject(spaceName, "secondProject", "BPM", "1");
//        bacService.updateProjectDescription(spaceName, "secondProject", "2");
//        bacService.updateProjectDescription(spaceName, "secondProject", "3");
//        bacService.listHistoryChanges(spaceName, "secondProject");

        bacService.createSpace("mySpace3");
        Collection<Space> spaces = bacService.getSpaces();
        System.out.println("spaces: " + spaces);

        bacService.shutdown();

        System.exit(0);
    }


}
