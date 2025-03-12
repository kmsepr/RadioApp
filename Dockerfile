FROM ubuntu:20.04
WORKDIR /app
RUN apt update && apt install -y openjdk-8-jdk wget unzip
RUN wget https://download.oracle.com/otn/java/javame/SDK/3.0.5/Java_ME_platform_SDK_3.0.5.zip && \
    unzip Java_ME_platform_SDK_3.0.5.zip -d /opt
ENV PATH="/opt/Java_ME_platform_SDK_3.0.5/bin:${PATH}"
COPY . /app
CMD ["/bin/bash"]
