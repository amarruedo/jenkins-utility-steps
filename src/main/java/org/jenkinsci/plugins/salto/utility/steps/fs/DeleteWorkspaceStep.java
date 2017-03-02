package org.jenkinsci.plugins.salto.utility.steps.fs;

import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collections;
import java.util.Set;

public class DeleteWorkspaceStep extends Step {

    @DataBoundConstructor public DeleteWorkspaceStep() {}

    @Override public StepExecution start(StepContext stepContext) throws Exception {
        return new DeleteWorkspaceStep.Execution(stepContext);
    }

    @Extension public static class DescriptorImpl extends StepDescriptor {

        @Override public String getFunctionName() {
            return "deleteWorkspace";
        }

        @Override public String getDisplayName() {
            return "Delete entire workspace, including temp folders";
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(FilePath.class);
        }
    }

    public static final class Execution extends SynchronousNonBlockingStepExecution<Void> {

        Execution(StepContext context) {
            super(context);
        }

        @Override protected Void run() throws Exception {

            String workspacePath = getContext().get(FilePath.class).getRemote();

            //delete workspace@script dir
            if (getContext().get(FilePath.class).child( workspacePath + "@script").exists())
                getContext().get(FilePath.class).child( workspacePath + "@script").deleteRecursive();

            //delete workspace@tmp dir
            if (getContext().get(FilePath.class).child( workspacePath + "@tmp").exists())
                getContext().get(FilePath.class).child( workspacePath + "@tmp").deleteRecursive();

            //delete workspace dir
            getContext().get(FilePath.class).deleteRecursive();

            return null;
        }

        private static final long serialVersionUID = 1L;
    }
}
