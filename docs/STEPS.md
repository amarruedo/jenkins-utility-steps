# Implemented Steps

## Docker

* `dockerParams` - Get uid, gid, docker gid ... to build a custom container on. Both `dockerConfigPath` and `attachSocket` are optional, being default values `${workspace}/.docker` and `false` respectively. It only works on Unix nodes.

```
node {

    def dockerParams = dockerParams(dockerConfigPath: 'my/tmp/folder', attachSocket: true)
    echo dockerParams.buildArgs
    echo dockerParams.runArgs
    
    def environment  = docker.build "myApp:buildConatiner", "${dockerParams.buildArgs} -f Dockerfile.build ."
    environment.inside ("${dockerParams.runArgs}") {
    
        ......
    
    }
}

```

### List of build args

* UID: build user ID 
* GID: build user group ID
* WORKSPACE: current jenkins build workspace path
* DOCKER_GID: docker group ID, to be used in docker in docker builds

### List of run args

* DOCKER_CONFIG: path to store/retrieve docker related config files

Optionally mounts docker socket for docker in docker builds (-v /var/run/docker.sock:/var/run/docker.sock).

## File System

* `writeBinaryFile` - Write binary file to workspace.

```
node {

    def myFile = ${base64EncodedFile}
    def filePath = pwd() + '/config.json'
    writeBinaryFile(file: filePath, data: myFile.decodeBase64())

}
```

* `deleteWorkspace` - Delete all folders related to the build job, including `${workspace}`, `${workspace}@script` and `${workspace}@tmp`

```
node {

    //cleanup
    deleteWorkspace()

}
```

## Git

* `gitParams` - Get git and application version related variables, like commit, short commit, stage etc. Version is computed based on git tags.

```
node {

  deleteWorkspace()
  checkout scm
  
  def gitParams = gitParams()
  echo gitParams.commit
  echo gitParams.shortCommit
  echo gitParams.buildNumber.toString()

  if(gitParams.release.isPresent)
  {
    echo gitParams.release.version
    echo gitParams.release.versionNoPrefix
    echo gitParams.release.versionShort
    echo gitParams.release.versionShortNoPrefix
    echo gitParams.release.major
    echo gitParams.release.minor
    echo gitParams.release.revision
    echo gitParams.release.stage
    echo gitParams.release.stageNumber
  }
  
  echo gitParams.dockerEnvs
}

```

### Tag format

Tag is based on [semantic versioning](http://semver.org/) and must follow the following pattern:

` x.y.z[-(alpha|beta|rc).[0-9]*]?`

* v3.2.0: `version=v3.2.0` `versionNoPrefix=3.2.0` `versionShort=v3.2.0`
* v3.2.0-beta.0: `version=v3.2.0-beta.0` `versionNoPrefix=3.2.0-beta.0` `versionShort=v3.2.0` `stage=beta` `stageNumber=0`

## Vault

* `secret` - Get a Vault secret. Returns a map with secret key/value pairs. `vaultAddress` is optional parameter to specify vault REST API endpoint address and port, default value is `vault.default.svc.cluster.local:8200`. HTTPS is not suported.

```
node {

     withCredentials([
        [
            $class: "UsernamePasswordMultiBinding",
            credentialsId: "vault-readonly-user",
            usernameVariable: "VAULT_USERNAME",
            passwordVariable: "VAULT_PASSWORD"
        ]
    ]) {
        def mySecret = secret(
            username: "$VAULT_USERNAME",
            password: "$VAULT_PASSWORD",
            secretName: "/mySecret"
        )
    }
    
    echo mySecret.mySecretKey
}
```

