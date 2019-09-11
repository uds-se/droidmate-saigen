FROM base:latest
# FROM debian:stretch-slim
# => couldn't use stretch-slim because of: `dpkg: dependency problems prevent configuration of ca-certificates-java`

# Based on a large extent on: 
# # https://github.com/sweisgerber-dev/android-sdk-ndk
# Helpful links:
# # https://hub.docker.com/r/thyrlian/android-sdk/

COPY *.sh /test/

RUN chmod +x /test/*.sh && \
    cd /test && \
    ./installDM.sh

CMD [ "bash" ]
