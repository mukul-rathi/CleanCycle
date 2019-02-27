FROM gradle

WORKDIR /home/gradle/project

# Copy all gradle related files
COPY *gradle* ./
#Copy Analytics package files
COPY src/main/java/CleanCycle/Analytics ./src/main/java/CleanCycle/Analytics/

COPY src/test/java/CleanCycle/Analytics ./src/test/java/CleanCycle/Analytics/
#Copy boot up bash scripts 
COPY ./analytics.sh ./ 

COPY ./map.json* ./

USER root
CMD ["./analytics.sh"] 
