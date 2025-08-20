# Releasing Helidon MCP

## Overview

The Helidon release workflow is triggered when a change is pushed to
a branch that starts with `release-`. The release workflow performs
a Maven release to the [Central Publishing Portal](https://central.sonatype.org/publish/publish-portal-guide/).
It does not currently do a GitHub release, so you must do that manually
using a script. Here is the overall flow:

1. Create a local release branch
2. Update CHANGELOG and verify version is correct
3. Push release branch to upstream, release workflow runs
4. Verify bits in Central Publishing Portal and then publish them
5. Create GitHub release
6. Increment version in main and update CHANGELOG


## Steps in detail

```shell
# Set this to the version you are releasing
export VERSION="1.0.0"
```

1. Create local release branch
    ```shell
    git clone git@github.com:oracle/helidon-mcp.git
    git checkout -b release-${VERSION}
    ```
2. Update local release branch
   1. Update version if needed (see below). For Milestone releases
      (4.0.0-M1) you'll want to change the version to something like
      4.0.0-M1-SNAPSHOT. For GA releases you probably don't need to
      touch the version at all (since it is probably already
      2.0.0-SNAPSHOT).
   2. Update `CHANGELOG`with latest info
   3. Commit changes locally

   If you need to update version:

   ```shell
      etc/scripts/release.sh --version=4.0.0-SNAPSHOT update_version
   ```

3. Push local release branch to upstream. This will trigger a release workflow.

    ```
    git push origin release-${VERSION}
    ```

4. Wait for release build to complete:

   https://github.com/helidon-io/helidon-mcp/actions/workflows/release.yaml

5. Check Central Portal for deployment
    1. In browser go to: https://central.sonatype.com/publishing and login as helidonrobot.
    2. Click on Deployments tab and you should see the Deployment listed (io-helidon-mcp-x.y.z)
    3. Status should be "Validated". You can explore the Deployment Info to see staged artifacts

6. Test staged bits. See Staging Repository Profile below

7. Release publishing: In the portal UI select the deployment then click Publish. Bits will eventually show up in maven central.

8.  Create GitHub release
   1. Create a fragment of the change log that you want used for the release
      description on the GitHub Releases page. Assume it is in `/tmp/frag.md`
   2. Set your API key (you generate this on your GitHub Settings):
      ```shell
      export GITHUB_API_KEY=<longhexnumberhere.....>
      ```
   3. Run script to create release in GitHub:
      ```shell
      etc/scripts/github-release.sh --changelog=/tmp/frag.md --version=${VERSION}
      ```
   4. Go to https://github.com/oracle/helidon-mcp/releases and verify release looks like
      you expect. You can edit it if you need to.

# Staging Repository Profile

To pull artifacts from the Central Portal staging repository add this to your `settings.xml`:

The BEARER_TOKEN must be that for the user that uploaded the release -- typically helidonrobot.
For general information concerning BEARER_TOKEN see 
* https://central.sonatype.org/publish/generate-portal-token/
* https://central.sonatype.org/publish/publish-portal-api/#authentication-authorization
* https://central.sonatype.org/publish/publish-portal-api/#manually-testing-a-deployment-bundle

```xml
  <servers>
   <server>
      <id>central.manual.testing</id>
      <configuration>
         <httpHeaders>
            <property>
               <name>Authorization</name>
               <value>Bearer ${BEARER_TOKEN}</value>
            </property>
         </httpHeaders>
      </configuration>
   </server>
</servers>

<profiles>
  <profile>
   <id>central.manual.testing</id>
   <repositories>
      <repository>
         <id>central.manual.testing</id>
         <name>Central Testing repository</name>
         <url>https://central.sonatype.com/api/v1/publisher/deployments/download</url>
      </repository>
   </repositories>
   <pluginRepositories>
      <pluginRepository>
         <id>central.manual.testing</id>
         <name>Central Testing repository</name>
         <url>https://central.sonatype.com/api/v1/publisher/deployments/download</url>
      </pluginRepository>
   </pluginRepositories>
  </profile>
</profiles>
```
