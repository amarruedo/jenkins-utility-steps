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

public class DockerParamsStep extends Step {

    private boolean attachSocket = false;
    private String dockerConfigPath = "";
    private String dns = "192.168.0.100";
    private String dnsSearch = "saltosystems.com";
    private final String ANDROID_SDK_GID = "1234";

    @DataBoundConstructor
    public DockerParamsStep(){ }

    @DataBoundSetter
    public void setAttachSocket(boolean attachSocket) { this.attachSocket = attachSocket; }

    @DataBoundSetter
    public void setDockerConfigPath(String dockerConfigPath) { this.dockerConfigPath = dockerConfigPath; }

    @DataBoundSetter
    public void setDns(String dns) { this.dns = dns; }

    @DataBoundSetter
    public void setDnsSearch(String dnsSearch)  { this.dnsSearch = dnsSearch; }

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

            if(!Functions.isWindows()) {
                // build args
                ByteArrayOutputStream userId = new ByteArrayOutputStream();
                launcher.launch().cmds("id", "-u").quiet(true).stdout(userId).start().joinWithTimeout(10, TimeUnit.SECONDS, launcher.getListener());

                ByteArrayOutputStream groupId = new ByteArrayOutputStream();
                launcher.launch().cmds("id", "-g").quiet(true).stdout(groupId).start().joinWithTimeout(10, TimeUnit.SECONDS, launcher.getListener());

                final String charsetName = Charset.defaultCharset().name();
                String buildArgs = "--build-arg UID=%s --build-arg GID=%s --build-arg WORKSPACE=%s";
                response.setBuildArgs(String.format(buildArgs, userId.toString(charsetName).trim(), groupId.toString(charsetName).trim(), filePath.getRemote()));

                // run args
                String dockerConfigPath = (step.dockerConfigPath != "" ? filePath.getRemote() + "/" + step.dockerConfigPath : filePath.getRemote() + "/.docker" );
                String runArgs = String.format("--dns=%s --dns-search=%s -e DOCKER_CONFIG=%s", step.dns, step.dnsSearch, dockerConfigPath);

                if(step.attachSocket) {

                    if(dockergid!="") runArgs += " --group-add=" + dockergid;

                    response.setRunArgs(runArgs + " -v /var/run/docker.sock:/var/run/docker.sock");
                }
                else
                    response.setRunArgs(runArgs + " --group-add=" + step.ANDROID_SDK_GID);

                return response;
            }
            else
                return response;
        }

        private static final long serialVersionUID = 1L;
    }

}
