from armv7/armhf-ubuntu

RUN apt-get update && apt-get install -y apt-utils
RUN apt-get install openjdk-8-jdk -y
RUN apt-get install -y curl nano tar sudo

ENV spark_ver 2.2.0

RUN curl http://www.us.apache.org/dist/spark/spark-${spark_ver}/spark-${spark_ver}-bin-hadoop2.7.tgz | tar -zx
RUN ln -s spark-${spark_ver}-bin-hadoop2.7 spark

ADD start-common.sh start-worker.sh start-master.sh /
ADD aws-java-sdk-1.7.4.jar hadoop-aws-2.7.3.jar s3.properties test.jar mc.jar /

RUN chmod +x /start-common.sh /start-master.sh /start-worker.sh
RUN chmod +x /aws-java-sdk-1.7.4.jar /hadoop-aws-2.7.3.jar /s3.properties /test.jar /mc.jar

RUN mkdir /eventLogging

ENV PATH $PATH:/spark/bin
