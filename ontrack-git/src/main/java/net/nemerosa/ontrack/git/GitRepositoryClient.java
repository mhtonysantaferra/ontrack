package net.nemerosa.ontrack.git;

import net.nemerosa.ontrack.git.model.GitCommit;
import net.nemerosa.ontrack.git.model.GitLog;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.function.Consumer;

/**
 * Defines a client for a Git repository.
 */
public interface GitRepositoryClient {

    /**
     * Makes sure the repository is synchronised with its remote location.
     *
     * @param logger Used to log messages during the synchronisation
     */
    void sync(Consumer<String> logger);

    /**
     * Checks if the given repository is compatible with this client. The remote, user name
     * and password must be checked.
     */
    boolean isCompatible(GitRepository repository);

    /**
     * Gets a graph Git log between two boundaries.
     *
     * @param from Commitish string
     * @param to   Commitish string
     * @return Git log
     */
    GitLog graph(String from, String to);

    /**
     * Gets the full hash for a commit
     */
    String getId(RevCommit revCommit);

    /**
     * Gets the abbreviated hash for a commit
     */
    String getShortId(RevCommit revCommit);

    /**
     * Consolidation for a commit
     */
    GitCommit toCommit(RevCommit revCommit);
}
