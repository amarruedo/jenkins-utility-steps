package org.jenkinsci.plugins.salto.utility.steps.git;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Set;

public class GitParamsStep extends Step {

    @DataBoundConstructor
    public GitParamsStep(){ }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(stepContext);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override public String getFunctionName() {
            return "gitParams";
        }

        @Override public String getDisplayName() {
            return "Get git and application version related data, like commit, short commit, stage etc.";
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, FilePath.class, TaskListener.class);
        }
    }

    public static final class Execution extends SynchronousNonBlockingStepExecution<GitParamsResponse> {

        Execution(StepContext context) {
            super(context);
        }

        @Override
        protected GitParamsResponse run() throws Exception {
            GitParamsResponse response = new GitUtils(getContext().get(FilePath.class),getContext().get(TaskListener.class)).getGitData();
            response.setBuildNumber(getContext().get(Run.class).number);
            return response;
        }

        private static final long serialVersionUID = 1L;
    }
}
