package org.jenkinsci.plugins.salto.utility.steps.vault;

import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;

import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

public class SecretStep extends Step {

    private @Nonnull String username;
    private @Nonnull String password;
    private @Nonnull String secretName;
    private String vaultAddress = DescriptorImpl.vaultAddress;

    @DataBoundConstructor
    public SecretStep(String username, String password, String secretName) {
        this.username = username;
        this.password = password;
        this.secretName = secretName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) { this.password = password; }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) { this.username = username; }

    public String getVaultAddress() { return vaultAddress; }

    @DataBoundSetter
    public void setVaultAddress(String vaultAddress) { this.vaultAddress = vaultAddress; }

    public String getSecretName() { return secretName; }

    public void setSecretName(String secretName) { this.secretName = secretName; }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public static final String vaultAddress = "vault.default.svc.cluster.local:8200";

        @Override public String getFunctionName() {
            return "secret";
        }

        @Override public String getDisplayName() {
            return "Get vault secret by path name";
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }
    }

    public static final class Execution extends SynchronousNonBlockingStepExecution<Map<String, Object>> {

        private transient final SecretStep step;

        Execution(SecretStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override protected Map<String, Object> run() throws Exception {

            VaultUtils v = new VaultUtils(getContext().get(TaskListener.class), step.vaultAddress);
            if(v.Authenticate(step.username,step.password)) {
                return v.GetSecrets(step.secretName);
            }
            else
            {
                throw new AbortException("Fail: Vault authentication failed ");
            }
        }

        private static final long serialVersionUID = 1L;
    }
}
