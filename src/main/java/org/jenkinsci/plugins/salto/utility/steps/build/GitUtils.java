package org.jenkinsci.plugins.salto.utility.steps.build;

import hudson.AbortException;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTag;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.util.*;

public class GitUtils implements Serializable{

    private static final long serialVersionUID = 1L;
    private FilePath filePath;
    private TaskListener listener;
    private static final String GIT_FOLDER = ".git";

    public GitUtils(FilePath filePath, TaskListener listener) throws IOException, GitAPIException, InterruptedException {

        final PrintStream logger = listener.getLogger();
        logger.println("********* Git var init ********");
        logger.println("GitVars: Attempting to find build folder in: " + filePath.child(GIT_FOLDER).getRemote() );

        this.filePath = filePath;
        this.listener = listener;
    }

    public BuildParamsResponse getGitData() throws IOException, GitAPIException, InterruptedException{

        final PrintStream logger = listener.getLogger();
        BuildParamsResponse result = filePath.act(new GitDataReader());
        if (result == null){
            logger.println("GitVars: Git repository info not found");
            throw new AbortException("Fail: GitVars empty file ");
        }
        else
        {
            return result;
        }
    }

    private static final class GitDataReader implements FilePath.FileCallable<BuildParamsResponse> {

        private static final long serialVersionUID = 1;
        private Repository repo;

        @Override
        public BuildParamsResponse invoke(File f, VirtualChannel channel) throws IOException {

            if (f == null) throw new AbortException("Fail: No repository found");

            this.repo = new FileRepository(f.getAbsolutePath() + "/" + GIT_FOLDER);

            if(repo == null) throw new AbortException("Fail: No repository found");

            // get commit info
            ObjectId lastCommitId = repo.resolve(Constants.HEAD);

            if(lastCommitId == null) throw new AbortException("Fail: Git commit not found");

            //get release info
            BuildParamsResponse.Release release = new BuildParamsResponse.Release();
            List<BuildParamsResponse.Release> releases = getReleasesByCommitId(lastCommitId, repo);

            if(releases.size() > 0)
            {
                Collections.sort(releases, new Comparator<BuildParamsResponse.Release>() {
                    public int compare(BuildParamsResponse.Release r1, BuildParamsResponse.Release r2) {
                        if (r1.GreaterThan(r2)) return 1;
                        else if (r1.LessThan(r2)) return -1;
                        else return 0;
                    }
                });

                release = releases.get(releases.size()-1);
            }

            BuildParamsResponse result = new BuildParamsResponse();
            result.setCommit(lastCommitId.getName());
            result.setShortCommit(lastCommitId.abbreviate( 7 ).name());
            result.setRelease(release);

            return  result;
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {

        }

        private List<BuildParamsResponse.Release> getReleasesByCommitId(ObjectId commitId, Repository repo) throws AbortException {
            Git git = new Git(repo);
            List<BuildParamsResponse.Release> tags = new ArrayList<>();
            try {
                List<Ref> list = git.tagList().call();
                for (Ref ref : list) {
                    Ref peeledRef = repo.peel(ref);
                    //si es null no es un annotated tag
                    if(peeledRef.getPeeledObjectId() != null) {
                        // annotated tags
                        if(commitId.equals(peeledRef.getPeeledObjectId()))
                            tags.add(new BuildParamsResponse.Release(ref.getName()));
                    }
                    else {
                        // lightweight tag
                        if(commitId.equals(peeledRef.getObjectId()))
                            tags.add(new BuildParamsResponse.Release(ref.getName()));
                    }
                }

                return tags;
            } catch (GitAPIException e) {
                throw new AbortException("Fail: GitAPIException " + e.toString());
            }
        }
    }
}
