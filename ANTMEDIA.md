# Manual publication to github

As this project lacks a CI pipeline, the current approach is to build the libraries locally
in the dev environment and publish them to ~/.m2

Then use these commands to deploy them to Github Packages:

Please note that deploying directly from under ~/.m2 is not allowed by Maven, so
you can see that the files have been manually copied to a temp directory as a workaround:


```
 mvn deploy:deploy-file ^
  -DrepositoryId=github ^
  -Durl=https://maven.pkg.github.com/ant-media/javacpp-presets ^
  -Dfile=C:\tmp\srt\1.5.3-1.5.11\srt-1.5.3-1.5.11.jar ^
  -DgeneratePom=false ^
  -Dfiles=C:\tmp\srt\1.5.3-1.5.11\srt-1.5.3-1.5.11-linux-x86_64.jar,C:\tmp\srt\1.5.3-1.5.11\srt-1.5.3-1.5.11-linux-arm64.jar,C:\tmp\srt\1.5.3-1.5.11\srt-1.5.3-1.5.11-macosx-x86_64.jar ^
  -Dclassifiers=linux-x86_64,linux-arm64,macosx-x86_64 ^
  -Dtypes=jar,jar
  
  
 mvn deploy:deploy-file ^
  -DrepositoryId=github ^
  -Durl=https://maven.pkg.github.com/ant-media/javacpp-presets ^
  -Dfile=C:\tmp\srt\1.5.3-1.5.11\srt-1.5.3-1.5.11-linux-x86_64.jar ^
  -DgroupId=org.bytedeco ^
  -DartifactId=srt ^
  -Dversion=1.5.3-1.5.11 ^
  -Dclassifier=linux-x86_64 ^
  -Dpackaging=jar ^
  -DgeneratePom=false
  
 mvn deploy:deploy-file ^
  -DrepositoryId=github ^
  -Durl=https://maven.pkg.github.com/ant-media/javacpp-presets ^
  -Dfile=C:\tmp\srt\1.5.3-1.5.11\srt-1.5.3-1.5.11-linux-arm64.jar ^
  -DgroupId=org.bytedeco ^
  -DartifactId=srt ^
  -Dversion=1.5.3-1.5.11 ^
  -Dclassifier=linux-arm64 ^
  -Dpackaging=jar ^
  -DgeneratePom=false
  
mvn deploy:deploy-file ^
  -DrepositoryId=github ^
  -Durl=https://maven.pkg.github.com/ant-media/javacpp-presets ^
  -Dfile=C:\tmp\srt\1.5.3-1.5.11\srt-1.5.3-1.5.11-macosx-x86_64.jar ^
  -DgroupId=org.bytedeco ^
  -DartifactId=srt ^
  -Dversion=1.5.3-1.5.11 ^
  -Dclassifier=linux-macosx-x86_64 ^
  -Dpackaging=jar ^
  -DgeneratePom=false
```


