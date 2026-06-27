# Development

## Contents Overview
- [Environment](#environment)
- [Publish a release](#publish-a-release)
   - [GitHub](#github)
   - [Maven Repository](#maven-repository)
   
## Environment
- Java 21   

## Publish a release

### GitHub
- Start in the project directory
- Search and solve all TODO entries
- Check and if necessary update the version and time stamp of all components
  comparison with the tag from the last release.
- Maven: check and update of dependencies
- __Use Java 21__
- Final test (Junit only)  
  Call `mvn clean test`
- Final test (JUnit only + GPG)  
  Call `mvn clean verify`
- __Provided all tests are successful!__
- Finalize version in the classes
- Finalize version in CHANGES
- Final update of CHANGES / README.md / pom.xml  
  Call `ant -f ./development/build.xml release`
  this also includes updating the version in `pom.xml`  
- Final commit of the release  
  Release x.x.x
- Create a tag without comments  

### Maven Repository
- __Based on the previous step__
- Call `ant -f ./development/build.xml publish`  
  see also https://central.sonatype.com/artifact/com.seanox/seanox-remote-deployment  
  see also https://mvnrepository.com/artifact/com.seanox/seanox-remote-deployment
