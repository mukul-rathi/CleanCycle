import psycopg2
from psycopg2 import sql
import json
import os 


columns = {
    "position": ["uuid","AccX","AccY", "AccZ", "Acc_mag", "Altitude", "GyroX", "GyroY",	"GyroZ", "Gyro_mag", "Latitude", "Longitude", "Speed"],
    "weather" : ["uuid","DewPt", "Humid", "MxWSpd", "Press", "Rain", "Sun", "Temp_CBS", "Temp_CL", "WindDr", "WindSp"],
    "time" :["uuid","Date", "Counter","Millis", "Start", "Time"],
    "system_status": ["uuid","BatteryVIN", "Satellites", "gpsUpdated", "nAcc"],
    "air_quality" : ["uuid","Latitude", "Longitude", "PM10","PM2.5"]
}

schemas = {
    "position": "(uuid TEXT PRIMARY KEY, AccX FLOAT8, AccY FLOAT8 , AccZ FLOAT8, Acc_mag FLOAT8, Altitude FLOAT8, GyroX FLOAT8, GyroY FLOAT8, GyroZ FLOAT8, Gyro_mag FLOAT8, Latitude FLOAT8, Longitude FLOAT8, Speed FLOAT8)",

     "weather" : "(uuid TEXT PRIMARY KEY, DewPt INTEGER, Humid INTEGER, MxWSpd INTEGER, Press INTEGER, Rain INTEGER, Sun FLOAT8, Temp_CBS FLOAT8, Temp_CL FLOAT8, WindDr INTEGER, WindSp INTEGER)",

    "time" : "(uuid TEXT PRIMARY KEY, Date INTEGER, Counter INTEGER,Millis INTEGER, Start TEXT, Time FLOAT8)",

    "system_status": " (uuid TEXT PRIMARY KEY, BatteryVIN FLOAT8, Satellites INTEGER, gpsUpdated INTEGER, nAcc INTEGER)",

    "air_quality" : "(uuid TEXT PRIMARY KEY, Latitude FLOAT8,  Longitude FLOAT8, PM10 FLOAT8, PM25 FLOAT8)"
}
class DBConnection:
    def __init__(self):
      #set up connection
      connectionParameters = "dbname=" + os.environ['POSTGRES_DB']
      connectionParameters += " user=" + os.environ['POSTGRES_USER']
      connectionParameters += " password=" + os.environ['POSTGRES_PASSWORD']
      connectionParameters += " host=host.docker.internal port=5432"
     
      self._conn = psycopg2.connect(connectionParameters)
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

  def getDBInfo(self): #print out all tables and their records
    tables = {}
    self._cur.execute("""SELECT table_name FROM information_schema.tables
       WHERE table_schema = 'public'""") # this gets an iterable collection of the public tables in the database
    for table in self._cur.fetchall():
      cur2 = self._conn.cursor()
      cur2.execute(sql.SQL("SELECT * FROM {} ;").format(sql.Identifier(table[0]))) # note sql module used for safe dynamic SQL queries 
      tables[table[0]] =  cur2.fetchall()
    return json.dumps(tables)

