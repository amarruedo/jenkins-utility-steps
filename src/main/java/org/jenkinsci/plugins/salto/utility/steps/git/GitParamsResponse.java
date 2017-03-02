package org.jenkinsci.plugins.salto.utility.steps.git;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitParamsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private int buildNumber;
    private String commit;
    private String shortCommit;
    private Release release;

    public GitParamsResponse(Release release, int buildNumber){
        this.release = release;
        this.buildNumber = buildNumber;
    }

    public GitParamsResponse(Release release){
        this.release = release;
    }

    public GitParamsResponse(){

    }

    @Whitelisted
    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    @Whitelisted
    public String getShortCommit() {
        return shortCommit;
    }

    public void setShortCommit(String shortCommit) {
        this.shortCommit = shortCommit;
    }

    @Whitelisted
    public int getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }

    @Whitelisted
    public Release getRelease() {
        return release;
    }

    public void setRelease(Release release)
    {
        this.release = release;
    }

    @Whitelisted
    public String getDockerEnvs() {
        return String.format("-e COMMIT=%s -e COMMIT_SHORT=%s -e BUILD_NUMBER=%d %s", commit ,shortCommit, buildNumber, release.getReleaseVars());
    }

    public static class Release implements Serializable {

        private static final long serialVersionUID = 1L;
        private static final String PATTERN = "^v(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-(0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(\\+[0-9a-zA-Z-]+(\\.[0-9a-zA-Z-]+)*)?$";

        private boolean isPresent = false;
        private boolean isAlpha = false;
        private boolean isBeta = false;
        private boolean isReleaseCandidate = false;
        private boolean isOfficial = false;
        private int major = 0;
        private int minor = 0;
        private int revision = 0;
        private String stage = "";
        private int stageNumber = 0;

        private TYPE tagType = TYPE.UNKNOWN;
        private String releaseVars = "-e VERSION= -e VERSION_NOPREFIX= -e VERSION_SHORT= -e VERSION_SHORT_NOPREFIX= -e VERSION_MAJOR= -e VERSION_MINOR= -e VERSION_REVISION= -e VERSION_STAGE= -e VERSION_STAGE_NUMBER=";
        private String versionReleaseVars = "-e VERSION=%s -e VERSION_NOPREFIX=%s -e VERSION_SHORT=%s -e VERSION_SHORT_NOPREFIX=%s -e VERSION_MAJOR=%d -e VERSION_MINOR=%d -e VERSION_REVISION=%d -e VERSION_STAGE= -e VERSION_STAGE_NUMBER=";

        public Release(String releaseTag){

            Pattern p = Pattern.compile(PATTERN);
            Matcher m = p.matcher(releaseTag);

            int i = 1;
            if(m.find())
            {
                isPresent = true;

                while(m.group(i) != null) {

                    switch (i) {

                        case 1:
                            major = new Integer(m.group(i));
                            break;

                        case 2:
                            minor = new Integer(m.group(i));
                            break;

                        case 3:
                            revision = new Integer(m.group(i));
                            break;

                        case 4:
                            stage = new String(m.group(i).getBytes());
                            break;

                        case 5:
                            stageNumber = new Integer(m.group(i));
                            break;
                    }

                    i++;
                }
            }

            computeType(i-1);
            computeVars(tagType);
        }

        public Release(){

        }

        @Whitelisted
        public boolean getIsPresent() {
            return isPresent;
        }

        @Whitelisted
        public boolean getIsAlpha() {
            return isAlpha;
        }

        @Whitelisted
        public boolean getIsBeta() {
            return isBeta;
        }

        @Whitelisted
        public boolean getIsReleaseCandidate() {
            return isReleaseCandidate;
        }

        @Whitelisted
        public boolean getIsOfficial() {
            return isOfficial;
        }

        @Whitelisted
        public int getMajor() {
            return major;
        }

        @Whitelisted
        public int getMinor() {
            return minor;
        }

        @Whitelisted
        public int getRevision() {
            return revision;
        }

        @Whitelisted
        public String getStage() {
            return stage;
        }

        @Whitelisted
        public int getStageNumber() {
            return stageNumber;
        }

        @Whitelisted
        public String getVersion(){

            String baseVersion = "v" + major + "." + minor + "." + revision;

            switch(tagType)
            {
                case VERSION:
                    return baseVersion;

                case STAGE:
                    return baseVersion + "-" + stage;

                case STAGE_NUMBER:
                    return baseVersion+ "-" + stage + "." + stageNumber;

                default:
                    return "";
            }
        }

        @Whitelisted
        public String getVersionNoPrefix(){
            return getVersion().substring(1);
        }

        @Whitelisted
        public String getVersionShort(){
            if( getVersion().indexOf("-") != -1 )
                return getVersion().substring(0, getVersion().indexOf("-"));
            else
                return getVersion();
        }

        @Whitelisted
        public String getVersionShortNoPrefix(){
            return getVersionShort().substring(1);
        }

        @Whitelisted
        public String getReleaseVars(){
            return releaseVars;
        }

        private enum TYPE {
            VERSION, STAGE, STAGE_NUMBER, UNKNOWN
        }

        private void computeType(int groupCount)
        {
            switch(groupCount)
            {
                case 3:
                    tagType = TYPE.VERSION;
                    break;
                case 4:
                    tagType = TYPE.STAGE;
                    break;
                case 5:
                    tagType = TYPE.STAGE_NUMBER;
                    break;
                default:
                    tagType = TYPE.UNKNOWN;
            }
        }

        private void computeVars(TYPE type)
        {
            String tmpVars = String.format(versionReleaseVars, getVersion(), getVersionNoPrefix(), getVersionShort(), getVersionShortNoPrefix(), getMajor(), getMinor(), getRevision());

            switch(type)
            {
                case VERSION:
                    releaseVars = tmpVars;
                    break;
                case STAGE:
                    releaseVars = String.format(tmpVars + " -e VERSION_STAGE=%s -e VERSION_STAGE_NUMBER=", getStage());
                    break;
                case STAGE_NUMBER:
                    releaseVars = String.format(tmpVars + " -e VERSION_STAGE=%s -e VERSION_STAGE_NUMBER=%d", getStage(), getStageNumber());
                    break;
            }
        }
    }
}
