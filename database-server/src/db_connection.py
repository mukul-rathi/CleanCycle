import psycopg2
import json

class DBConnection:
    def __init__(self):
        #set up connection
        self._conn = psycopg2.connect("dbname=testdb user=testuser password=testpwd host=host.docker.internal port=5432")
        self._cur = self._conn.cursor()

    def createTable(self):
      self._cur.execute("CREATE TABLE test (id serial PRIMARY KEY, num integer, data varchar);")
      self._conn.commit()


    def insertItem(self):
      self._cur.execute("INSERT INTO test (num, data) VALUES (%s, %s)",(100, "abc'def"))
      self._conn.commit()


    def queryAll(self):
      self._cur.execute("SELECT * FROM test;")
      return json.dumps(self._cur.fetchall())

    def getConnectionStats(self):
        return json.dumps(self._conn.get_dsn_parameters())

