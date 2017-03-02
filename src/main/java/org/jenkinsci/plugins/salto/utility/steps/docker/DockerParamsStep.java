package org.jenkinsci.plugins.salto.utility.steps.docker;

import com.google.common.collect.ImmutableSet;
import hudson.*;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by amarruedo on 28/02/17.
 */
public class DockerParamsStep extends Step {

    private boolean attachSocket = false;
    private String dockerConfigPath = "";

    @DataBoundConstructor
    public DockerParamsStep(){ }

    @DataBoundSetter
    public void setAttachSocket(boolean attachSocket) { this.attachSocket = attachSocket; }

    @DataBoundSetter
    public void setDockerConfigPath(String dockerConfigPath) { this.dockerConfigPath = dockerConfigPath; }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new DockerParamsStep.Execution(this, stepContext);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override public String getFunctionName() {
            return "dockerParams";
        }

        @Override public String getDisplayName() {
            return "Get uid, gid, docker gid ... to build a custom container.";
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Node.class, Launcher.class, FilePath.class, TaskListener.class);
        }
    }

    public static final class Execution extends SynchronousNonBlockingStepExecution<DockerParamsResponse> {

        private transient final DockerParamsStep step;
        private static final String DOCKER_GID = "DOCKER_GID" ;

        Execution(DockerParamsStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected DockerParamsResponse run() throws Exception {
            return getArgs(getContext().get(Node.class),getContext().get(Launcher.class), getContext().get(FilePath.class), getContext().get(TaskListener.class));
        }

        private DockerParamsResponse getArgs(Node node,Launcher launcher, FilePath filePath, TaskListener listener) throws IOException, InterruptedException {

            final PrintStream logger = listener.getLogger();
            boolean win = Functions.isWindows();
            String buildArgs = "--build-arg UID=%s --build-arg GID=%s --build-arg WORKSPACE=%s --build-arg DOCKER_GID=%s";

            DockerParamsResponse response = new DockerParamsResponse();

            String dockergid = "";
            List<EnvironmentVariablesNodeProperty> envVars = node.getNodeProperties().getAll(EnvironmentVariablesNodeProperty.class);
            for (int i= 0; i< envVars.size(); i++) {
                if(envVars.get(i).getEnvVars().containsKey(DOCKER_GID)) {
                    dockergid = envVars.get(i).getEnvVars().get(DOCKER_GID);
                    break;
                }
            }

            logger.println("DOCKER_GID: " + dockergid);

            if(!win) {
                String dockerConfigPath = (step.dockerConfigPath != "" ? filePath.getRemote() + "/" + step.dockerConfigPath : filePath.getRemote() + "/.docker" );
                String runArgs = String.format("-e DOCKER_CONFIG=%s", dockerConfigPath);
                ByteArrayOutputStream userId = new ByteArrayOutputStream();
                launcher.launch().cmds("id", "-u").quiet(true).stdout(userId).start().joinWithTimeout(10, TimeUnit.SECONDS, launcher.getListener());

                ByteArrayOutputStream groupId = new ByteArrayOutputStream();
                launcher.launch().cmds("id", "-g").quiet(true).stdout(groupId).start().joinWithTimeout(10, TimeUnit.SECONDS, launcher.getListener());

                final String charsetName = Charset.defaultCharset().name();
                response.setBuildArgs(String.format(buildArgs, userId.toString(charsetName).trim(), groupId.toString(charsetName).trim(), filePath.getRemote(), dockergid));

                if(step.attachSocket)
                    response.setRunArgs(String.format("%s -v /var/run/docker.sock:/var/run/docker.sock", runArgs));
                else
                    response.setRunArgs(runArgs);

                return response;
            }
            else
            {
                return response;
            }
        }

        private static final long serialVersionUID = 1L;
    }

}
