FROM gradle

WORKDIR /home/gradle/project

# Copy all gradle related files
COPY *gradle* ./
#Copy Analytics package files
COPY src/main/java/CleanCycle/Analytics ./src/main/java/CleanCycle/Analytics/

COPY src/test/java/CleanCycle/Analytics ./src/test/java/CleanCycle/Analytics/
#Copy dependency
COPY database-server/tests/wait-for-it.sh ./

USER root
CMD ["./wait-for-it.sh", "endpoint:5000", "--timeout=30","--","gradle", "build"]
