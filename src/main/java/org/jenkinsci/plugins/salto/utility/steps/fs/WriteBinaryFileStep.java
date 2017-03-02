package org.jenkinsci.plugins.salto.utility.steps.fs;

import hudson.Extension;
import hudson.FilePath;
import java.util.Collections;
import java.util.Set;
import java.io.ByteArrayInputStream;


import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

public class WriteBinaryFileStep extends Step {

    private final String file;
    private final byte[] data;

    @DataBoundConstructor public WriteBinaryFileStep(String file, byte[] data) {
        this.file = file;
        this.data = data;
    }

    public String getFile() {
        return file;
    }

    public byte[] getData() {
        return data;
    }

    @Override public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }

    @Extension public static class DescriptorImpl extends StepDescriptor {

        @Override public String getFunctionName() {
            return "writeBinaryFile";
        }

        @Override public String getDisplayName() {
            return "Write binary file to workspace";
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(FilePath.class);
        }

    }

    public static final class Execution extends SynchronousNonBlockingStepExecution<Void> {

        private transient final WriteBinaryFileStep step;

        Execution(WriteBinaryFileStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override protected Void run() throws Exception {
            getContext().get(FilePath.class).child(step.file).copyFrom(new ByteArrayInputStream(step.data));
            return null;
        }

        private static final long serialVersionUID = 1L;

    }

}