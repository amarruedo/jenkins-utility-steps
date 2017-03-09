package org.jenkinsci.plugins.salto.utility.steps.git;

import hudson.AbortException;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.util.Map;

/**
 * Created by amarruedo on 23/02/17.
 */
public class GitUtils implements Serializable{

    private static final long serialVersionUID = 1L;
    private FilePath filePath;
    private TaskListener listener;

    public GitUtils(FilePath filePath, TaskListener listener) throws IOException, GitAPIException, InterruptedException {

        final PrintStream logger = listener.getLogger();
        logger.println("********* Git var init ********");
        logger.println("GitVars: Attempting to find git folder in: " + filePath.child(".git").getRemote() );

        this.filePath = filePath;
        this.listener = listener;
    }

    public GitParamsResponse getGitData() throws IOException, GitAPIException, InterruptedException{

        final PrintStream logger = listener.getLogger();
        GitParamsResponse result = filePath.act(new GitDataReader());
        if (result == null){
            logger.println("GitVars: Git repository info not found");
            throw new AbortException("Fail: GitVars empty file ");
        }
        else
        {
            return result;
        }
    }

    private static final class GitDataReader implements FilePath.FileCallable<GitParamsResponse> {

        private static final long serialVersionUID = 1;
        private Repository repo;

        @Override public GitParamsResponse invoke(File f, VirtualChannel channel) throws IOException {

            if (f == null)
                throw new AbortException("Fail: GitVars empty file ");

            this.repo = new FileRepository(f.getAbsolutePath() + "/.git");

            if(repo == null)
                throw new AbortException("Fail: GitVars no repository ");

            // get commit info
            ObjectId lastCommitId = repo.resolve(Constants.HEAD);

            if(lastCommitId == null) {
                throw new AbortException("Fail: GitVars commit not found ");
            }

            //get release info
            Git git = new Git(repo);
            // TODO: mirar otra manera de sacar los tags para poder detectar cuando un commit esta tageado mas de una vez
            GitParamsResponse.Release release = new GitParamsResponse.Release();
            Map<ObjectId,String> names = null;
            try {
                names = git.nameRev().add( lastCommitId ).addPrefix( "refs/tags/" ).call();
            } catch (GitAPIException e) {
                throw new AbortException("Fail: GitVars GitAPIException " + e.toString());
            }
            if(names.containsKey(lastCommitId))
                release = new GitParamsResponse.Release(names.get(lastCommitId));

            GitParamsResponse result = new GitParamsResponse();
            result.setCommit(lastCommitId.getName());
            result.setShortCommit(lastCommitId.abbreviate( 7 ).name());
            result.setRelease(release);

            return  result;
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {

        }
    }
}
