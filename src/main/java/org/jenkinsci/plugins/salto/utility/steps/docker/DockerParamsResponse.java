package org.jenkinsci.plugins.salto.utility.steps.docker;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;

public class DockerParamsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String buildArgs = "";
    private String runArgs = "";

    @Whitelisted
    public String getBuildArgs() {
        return buildArgs;
    }

    public void setBuildArgs(String buildArgs) {
        this.buildArgs = buildArgs;
    }

    @Whitelisted
    public String getRunArgs() {
        return runArgs;
    }

    public void setRunArgs(String runArgs) {
        this.runArgs = runArgs;
    }

}
