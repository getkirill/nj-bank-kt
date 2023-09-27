FROM eclipse-temurin:19
RUN mkdir /opt/bank
COPY ./build/libs/nj-bank-kt-1.0-SNAPSHOT-all.jar /opt/bank/app.jar
CMD ["java", "-jar", "/opt/bank/app.jar"]