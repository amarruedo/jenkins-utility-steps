package org.jenkinsci.plugins.salto.utility.steps.build;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.joda.time.DateTime;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Set;

public class BuildParamsStep extends Step {

    @DataBoundConstructor
    public BuildParamsStep(){ }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(stepContext);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override public String getFunctionName() {
            return "buildParams";
        }

        @Override public String getDisplayName() {
            return "Get build related data, like commit, short commit, build number, stage etc.";
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, FilePath.class, TaskListener.class);
        }
    }

    public static final class Execution extends SynchronousNonBlockingStepExecution<BuildParamsResponse> {

        // max int32 value = 2147483647
        // nos da un rango de 68 años de build numbers únicos y ascendentes, con precisión de 1 segundo.
        private final DateTime referenceDate = new DateTime(2017, 3, 1, 0, 0, 0);

        Execution(StepContext context) {
            super(context);
        }

        @Override
        protected BuildParamsResponse run() throws Exception {
            BuildParamsResponse response = new GitUtils(getContext().get(FilePath.class),getContext().get(TaskListener.class)).getGitData();
            response.setBuildNumber(getBuildNumber());
            return response;
        }

        private static final long serialVersionUID = 1L;

        private int getBuildNumber(){
            DateTime now = new DateTime();
            return org.joda.time.Seconds.secondsBetween(referenceDate, now).getSeconds();
        }
    }
}
