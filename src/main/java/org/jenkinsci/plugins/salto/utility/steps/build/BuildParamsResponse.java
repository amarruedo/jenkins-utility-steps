package org.jenkinsci.plugins.salto.utility.steps.build;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildParamsResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    private int buildNumber;
    private String commit;
    private String shortCommit;
    private Release release;

    public BuildParamsResponse(Release release, int buildNumber){
        this.release = release;
        this.buildNumber = buildNumber;
    }

    public BuildParamsResponse(Release release){ this.release = release; }

    public BuildParamsResponse(){}

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

        String result = "";

        for (Map.Entry<String, String> e: getEnvsMap().entrySet())
            result += " -e " + e.getKey() + "=" + e.getValue();

        return result.trim();
    }

    @Whitelisted
    public Envs getEnvs() {

        Properties properties = new Properties();

        for (Map.Entry<String, String> e: getEnvsMap().entrySet()){
            properties.setProperty(e.getKey(), e.getValue());
        }

        return new Envs(properties);
    }

    private Map<String,String> getEnvsMap(){

        Map<String, String>  envMap = new HashMap<String,String>(){{
            put("COMMIT", commit);
            put("COMMIT_SHORT", shortCommit);
            put("BUILD_NUMBER", String.valueOf(buildNumber));
        }};

        if(release.isPresent)
            envMap.putAll(release.getReleaseVarMap());

        return envMap;
    }

    public static class Release implements Serializable {

        private static final long serialVersionUID = 1L;
        private static final String PATTERN = "^v(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-(0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(\\+[0-9a-zA-Z-]+(\\.[0-9a-zA-Z-]+)*)?$";
        private boolean isPresent = false;
        private String version = "";
        private int major = 0;
        private int minor = 0;
        private int revision = 0;
        private String stage = "";
        private int stageNumber = 0;
        private int[] segments;
        private TYPE stageType = TYPE.UNKNOWN;
        private Map<String, String> releaseVariables;

        public Release(String releaseTag){

            Pattern p = Pattern.compile(PATTERN);
            Matcher m = p.matcher(releaseTag);
            boolean stageNumberPresent = false;

            if(m.find()) {

                String[] pieces = releaseTag.split("\\.");
                isPresent = true;
                version = releaseTag;
                major = new Integer(pieces[0].substring(1));
                minor = new Integer(pieces[1]);
                segments = new int[]{major, minor,revision};

                if(pieces[2].contains("-")) {

                    String[] subPieces = pieces[2].split("-");
                    revision = new Integer(subPieces[0]);
                    stage = subPieces[1];

                    if (stage.equalsIgnoreCase("alpha"))
                        stageType = TYPE.ALPHA;
                    else if(stage.equalsIgnoreCase("beta"))
                        stageType = TYPE.BETA;
                    else if(stage.equalsIgnoreCase("rc"))
                        stageType = TYPE.RC;
                    else
                        stageType = TYPE.UNKNOWN;
                }
                else {
                    revision = new Integer(pieces[2]);
                    stageType = TYPE.OFFICIAL;
                }

                if(pieces.length > 3) {
                    stageNumberPresent = true;
                    stageNumber = new Integer(pieces[3]);
                }
            }

            releaseVariables = new HashMap<String, String>(){{
                put("VERSION_IS_PRESENT", getIsPresent() ? "1" : "0");
                put("VERSION_IS_ALPHA", getIsAlpha() ? "1" : "0");
                put("VERSION_IS_BETA", getIsBeta() ? "1": "0");
                put("VERSION_IS_RELEASE_CANDIDATE", getIsReleaseCandidate() ? "1" : "0");
                put("VERSION_IS_OFFICIAL", getIsOfficial() ? "1" : "0");
                put("VERSION", getVersion());
                put("VERSION_NOPREFIX", getVersionNoPrefix());
                put("VERSION_SHORT", getVersionShort());
                put("VERSION_SHORTNOPREFIX", getVersionShortNoPrefix());
                put("VERSION_MAJOR", String.valueOf(getMajor()));
                put("VERSION_MINOR", String.valueOf(getMinor()));
                put("VERSION_REVISION", String.valueOf(getRevision()));
                put("VERSION_STAGE", getStage());
            }};

            releaseVariables.put("VERSION_STAGE_NUMBER", stageNumberPresent ? String.valueOf(getStageNumber()) : "");
        }

        public Release(){}

        @Whitelisted
        public boolean getIsPresent() {
            return isPresent;
        }

        @Whitelisted
        public boolean getIsAlpha() { return stageType == TYPE.ALPHA; }

        @Whitelisted
        public boolean getIsBeta() {
            return stageType == TYPE.BETA;
        }

        @Whitelisted
        public boolean getIsReleaseCandidate() {
            return stageType == TYPE.RC;
        }

        @Whitelisted
        public boolean getIsOfficial()  {
            return stageType == TYPE.OFFICIAL;
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
        public String getVersion(){ return version; }

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
        public String getVersionShortNoPrefix(){ return getVersionShort().substring(1); }

        @Whitelisted
        public String getReleaseVars(){
            String releaseVars = "";

            for (Map.Entry<String, String> e: releaseVariables.entrySet())
                releaseVars += " -e " + e.getKey() + "=" + e.getValue();

            return releaseVars.trim();
        }

        public Map<String, String> getReleaseVarMap(){
            return releaseVariables;
        }

        public int[] getSegments(){
            return segments;
        }

        public TYPE getStageType(){
            return  stageType;
        }

        public boolean GreaterThan(Release other)
        {
            return this.compare(other) > 0;
        }

        public boolean LessThan(Release other)
        {
            return this.compare(other) < 0;
        }

        public boolean Equal(Release other)
        {
            return this.compare(other) == 0;
        }

        private int compare(Release other) {

            if (getVersion().equalsIgnoreCase(other.getVersion()))
                return 0;

            if (Arrays.equals(getSegments(), other.getSegments())){

                if(stage.equalsIgnoreCase(other.getStage())){

                    if (stageNumber == other.getStageNumber())
                        return 0;
                    if (stageNumber > other.getStageNumber())
                        return 1;
                    if (stageNumber < other.getStageNumber())
                        return -1;
                }

                if(stage == "" && other.getStage() != "")
                    return 1;

                if(other.getStage() == "" && stage != "")
                    return -1;

                return comparePrereleases( other );
            }

            // comparar los mayor.minor.patch
            for(int i =0; i < 3 ; i++)
            {
                if(segments[i] > other.getSegments()[i])
                    return 1;

                if(segments[i] < other.getSegments()[i])
                    return -1;
            }

            return 0;
        }

        private int comparePrereleases(Release other){

           if (stageType.getCode() > other.getStageType().getCode())
               return 1;
           else if (stageType.getCode() < other.getStageType().getCode())
               return -1;
           else
           {
               if(stageNumber > other.getStageNumber())
                   return 1;
               else if (stageNumber < other.getStageNumber())
                   return -1;
               else
                   return 0;
           }
         }

        public enum TYPE {
            UNKNOWN(0),ALPHA(1), BETA(2), RC(3),OFFICIAL(4);

            private int code;

            TYPE(int c) { code = c; }

            public int getCode() { return code; }
        }
    }

    public static class Envs implements Serializable {

        private static final long serialVersionUID = 1L;
        private Properties properties ;

        public Envs(Properties properties){
            this.properties = properties;
        }

        @Whitelisted
        public Object get(String name) {
            if (!properties.containsKey(name)) {
                throw new NoSuchElementException();
            }

            return properties.get(name);
        }

        @Whitelisted
        public byte[] toByteArray() {
            byte[] result = null;
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                properties.store(bout, "Build parameters as environment variables");
                result = bout.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
    }
}
