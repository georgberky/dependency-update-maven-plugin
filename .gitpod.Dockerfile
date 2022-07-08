FROM gitpod/workspace-full

USER gitpod

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && sdk install java 8.0.302-open && sdk default java 8.0.302-open && sdk install maven 3.8.6 && sdk default maven 3.8.6"
