FROM python:3.6

WORKDIR  /usr/src/app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
RUN mkdir -p /usr/backups
RUN chmod +x ./wait-for-it.sh
RUN chmod +x ./db-tests.sh

CMD ["./db-tests.sh"]