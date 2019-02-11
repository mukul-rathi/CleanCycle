import psycopg2
from flask import Flask
app = Flask(__name__)
import json

@app.route('/')
def hello_world():
    connection = psycopg2.connect("dbname=testdb user=testuser password=testpwd host=host.docker.internal port=5432")
    cursor = connection.cursor()
    return json.dumps(connection.get_dsn_parameters())


if __name__ == '__main__':
    app.run(debug=True,host='0.0.0.0', port=80)