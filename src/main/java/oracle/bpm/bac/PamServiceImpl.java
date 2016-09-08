package oracle.bpm.bac;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.jcr.repository.RepositoryImpl;
import org.apache.jackrabbit.oak.plugins.document.DocumentMK;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;

public class PamServiceImpl {

    public static final String PAM_ROOT = "pam-spaces";
    public static final String MIX_VERSIONABLE = "mix:versionable";
    public static final String MIX_LOCKABLE = "mix:lockable";
    private final Repository repo;
    private final DocumentNodeStore ns;

    public PamServiceImpl() throws UnknownHostException, RepositoryException {
        DB db = new MongoClient("127.0.0.1", 27017).getDB("oak");
        ns = new DocumentMK.Builder().
                setMongoDB(db).getNodeStore();
        repo = new Jcr(new Oak(ns)).createRepository();

        Session session = repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
        if(!session.getRootNode().hasNode(PAM_ROOT)) {
            session.getRootNode().addNode(PAM_ROOT);
            session.save();
        }

        session.logout();
    }

    public void createUser(@NotNull String userName) throws RepositoryException {
        Session session = repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));

        UserManager userManager = ((JackrabbitSession) session).getUserManager();
        userManager.createUser(userName, "welcome1");

        System.out.println("created user " + userName);

        session.save();
        session.logout();

    }

    public void shareSpace(@NotNull String spaceId, @NotNull String userName) throws RepositoryException {
        Session session = repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));

        Node pamRoot = session.getRootNode().getNode(PAM_ROOT);
        String spacePath = pamRoot.getNode(spaceId).getPath();

        AccessControlManager aMgr = session.getAccessControlManager();
        Privilege[] privileges = new Privilege[] { aMgr.privilegeFromName(Privilege.JCR_ALL) };

        AccessControlList acl = AccessControlUtils.getAccessControlList(aMgr, spacePath);

        acl.addAccessControlEntry(new PrincipalImpl(userName), privileges);

        aMgr.setPolicy(spacePath, acl);

        session.save();
        session.logout();
    }

    @NotNull
    public Space createSpace(@NotNull String spaceId) throws RepositoryException {
        Session session = repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
        Node pamRoot = session.getRootNode().getNode(PAM_ROOT);
        String userID = session.getUserID();
        if (!pamRoot.hasNode(spaceId)) {
            Node newSpace = pamRoot.addNode(spaceId);
            newSpace.addMixin(MIX_VERSIONABLE);
            newSpace.setProperty("creator", userID);

            AccessControlUtils.denyAllToEveryone(session, newSpace.getPath());

            session.save();
            System.out.println("Space " + spaceId + " created");
        }

        Version version = session.getWorkspace().getVersionManager().getBaseVersion("/" + PAM_ROOT + "/" + spaceId);
        Calendar creationDate = version.getCreated();

        session.logout();

        return new Space(spaceId, creationDate, userID);
    }

    public void removeSpace(@NotNull String spaceId) throws RepositoryException {
        Session session = repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
        Node pamRoot = session.getRootNode().getNode(PAM_ROOT);
        if (pamRoot.hasNode(spaceId)) {
            pamRoot.getNode(spaceId).remove();
            session.save();
            System.out.println("Space " + spaceId + " removed");
        }

        session.logout();
    }

    @Nullable
    public Space getSpace(@NotNull String spaceId) throws RepositoryException {
        Space result = null;

        Session session = repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
        Node pamRoot = session.getRootNode().getNode(PAM_ROOT);
        if (pamRoot.hasNode(spaceId)) {
            Node spaceNode = pamRoot.getNode(spaceId);
            result = nodeToSpace(session, spaceNode);
        }

        session.logout();

        return result;
    }

    @NotNull
    public Collection<Space> getSpaces() throws RepositoryException {
        HashSet<Space> result = new HashSet<Space>();

        Session session = repo.login(
                new SimpleCredentials("jcooper", "welcome1".toCharArray()));
        Node pamRoot = session.getRootNode().getNode(PAM_ROOT);

        NodeIterator iterator = pamRoot.getNodes();
        while (iterator.hasNext()) {
            Node node = iterator.nextNode();

            // filter out permissions folder
            if(!node.getName().equals("rep:policy")) {
                result.add(nodeToSpace(session, node));
            }
        }

        session.logout();

        return result;
    }

    @NotNull
    public Project createProject(@NotNull String spaceId, @NotNull String projectId, @NotNull String type, @Nullable String description) throws RepositoryException {
        Session session = repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
        Node pamRoot = session.getRootNode().getNode(PAM_ROOT);

        if (!pamRoot.hasNode(spaceId)) {
            throw new IllegalArgumentException("Space " + spaceId + " does not exists");
        }

        Node projectNode = pamRoot.getNode(spaceId).addNode(projectId);
        projectNode.addMixin(MIX_VERSIONABLE);
        projectNode.addMixin(MIX_VERSIONABLE);
        projectNode.setProperty("type", type);
        projectNode.setProperty("description", description);
        String userID = session.getUserID();
        projectNode.setProperty("creator", userID);

        session.save();

        System.out.println("Project " + projectId + " created");

        Version version = session.getWorkspace().getVersionManager().getBaseVersion("/" + PAM_ROOT + "/" + spaceId + "/" + projectId);
        Calendar creationDate = version.getCreated();

        session.logout();

        return new Project(spaceId, projectId, creationDate, userID, description);
    }

    public void removeProject(@NotNull String projectId) {

    }

    public boolean existsProject(@NotNull String projectId) {
        return false;
    }

    @NotNull
    public Project getProject(@NotNull  String spaceId, @NotNull String projectId) {
        return null;
    }

    @NotNull
    public void listHistoryChanges(@NotNull String spaceId, @NotNull String projectId) throws RepositoryException {
        Session session = repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
        Node pamRoot = session.getRootNode().getNode(PAM_ROOT);

        String projectRelPath = spaceId + "/" + projectId;
        if(!pamRoot.hasNode(projectRelPath)){
            throw new IllegalArgumentException("Project " + projectId + "does not exists");
        }

        Node projectNode = pamRoot.getNode(projectRelPath);
        VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(projectNode.getPath());

        VersionIterator allVersions = versionHistory.getAllVersions();
        while (allVersions.hasNext()) {
            Version version = allVersions.nextVersion();

            // rootVersion is always empty, look https://developer.jboss.org/thread/239790?start=0&tstart=0
            if(version.getName().equals("jcr:rootVersion")) {
                // do nothing here
//                System.out.println(nodeToProject(session, projectNode));
            } else {
                Calendar created = version.getCreated();
                Node node = version.getFrozenNode();
                System.out.println("Version: " + version.getName() + " => " + nodeToProject(node, created).toString());
            }

        }

        session.logout();
    }

    @NotNull
    public Collection<Project> getProjects(@NotNull String spaceId) {
        return null;
    }

    @NotNull
    public void updateProjectDescription(@NotNull String spaceId, @NotNull String projectId, @Nullable String description) throws RepositoryException {
        Session session = repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
        Node pamRoot = session.getRootNode().getNode(PAM_ROOT);

        String projectRelPath = spaceId + "/" + projectId;
        if(!pamRoot.hasNode(projectRelPath)){
            throw new IllegalArgumentException("Project " + projectId + " does not exists");
        }

        Node projectNode = pamRoot.getNode(projectRelPath);
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        versionManager.checkout(projectNode.getPath());

        projectNode.setProperty("description", description);

        session.save();
        versionManager.checkin(projectNode.getPath());

        session.logout();

    }

    @NotNull
    private Project nodeToProject(@NotNull Session session, @NotNull Node node) throws RepositoryException {
        Project result;

        Version version = session.getWorkspace().getVersionManager().getBaseVersion(node.getPath());
        Calendar creationDate = version.getCreated();
        String spaceId = node.getParent().getName();
        String creator = node.getProperty("creator").getString();
        String description = node.getProperty("description").getString();

        result = new Project(spaceId, node.getName(), creationDate, creator, description);

        return result;
    }

    @NotNull
    private Project nodeToProject(@NotNull Node frozenNode, @NotNull Calendar creationDate) throws RepositoryException {
        Project result;

        String spaceId = frozenNode.getParent().getName();
        String creator = frozenNode.getProperty("creator").getString();
        String description = frozenNode.getProperty("description").getString();

        result = new Project(spaceId, frozenNode.getName(), creationDate, creator, description);

        return result;
    }

    @NotNull
    private Space nodeToSpace(@NotNull Session session, @NotNull Node spaceNode) throws RepositoryException {
        Space result;

        Version version = session.getWorkspace().getVersionManager().getBaseVersion(spaceNode.getPath());
        Calendar creationDate = version.getCreated();
        String creator = spaceNode.getProperty("creator").getString();

        result = new Space(spaceNode.getName(), creationDate, creator);

        return result;
    }

    public void shutdown() {
        System.out.println("Shutting down repo");
        ns.dispose();
        ((RepositoryImpl)repo).shutdown();
    }

}
