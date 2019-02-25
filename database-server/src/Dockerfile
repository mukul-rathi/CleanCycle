FROM python:3.6

WORKDIR  /usr/src/app
COPY requirements.txt ./
RUN mkdir -p /usr/backups
COPY ./backups /usr/backups
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD [ "python", "./bootstrap.py" ]

