FROM python:3.6

WORKDIR  /usr/src/app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
RUN chmod +x ./wait-for-it.sh
CMD ["./wait-for-it.sh", "endpoint:5000", "--timeout=30","--","pytest"]
#used for code style
#CMD ["pylint", "-rn","--disable=C0303,C0301", "/usr/src/app/"]
