import psycopg2
from psycopg2 import sql
import sqlalchemy
from sqlalchemy import create_engine
import json
import os
import pandas as pd
import uuid
import io
import time
from random import randint

columns = {
    "position": [
        "uuid", "AccX", "AccY", "AccZ", "Acc_mag", "Altitude", "GyroX",
        "GyroY", "GyroZ", "Gyro_mag", "Latitude", "Longitude", "Speed"
    ],
    "weather": [
        "uuid", "DewPt", "Humid", "MxWSpd", "Press", "Rain", "Sun", "Temp_CBS",
        "Temp_CL", "WindDr", "WindSp"
    ],
    "time": ["uuid", "Date", "Counter", "Millis", "Start", "Time"],
    "system_status":
    ["uuid", "BatteryVIN", "Satellites", "gpsUpdated", "nAcc"],
    "air_quality": ["uuid", "Latitude", "Longitude", "PM10", "PM2.5"]
}

schemas = {
    "position":
    "(uuid TEXT PRIMARY KEY, AccX FLOAT8, AccY FLOAT8 , AccZ FLOAT8, Acc_mag FLOAT8, Altitude FLOAT8, GyroX FLOAT8, GyroY FLOAT8, GyroZ FLOAT8, Gyro_mag FLOAT8, Latitude FLOAT8, Longitude FLOAT8, Speed FLOAT8)",
    "weather":
    "(uuid TEXT PRIMARY KEY, DewPt FLOAT8, Humid FLOAT8, MxWSpd FLOAT8, Press FLOAT8, Rain FLOAT8, Sun FLOAT8, Temp_CBS FLOAT8, Temp_CL FLOAT8, WindDr FLOAT8, WindSp FLOAT8)",
    "time":
    "(uuid TEXT PRIMARY KEY, Date FLOAT8, Counter FLOAT8,Millis FLOAT8, Start TEXT, Time FLOAT8)",
    "system_status":
    " (uuid TEXT PRIMARY KEY, BatteryVIN FLOAT8, Satellites FLOAT8, gpsUpdated FLOAT8, nAcc FLOAT8)",
    "air_quality":
    "(uuid TEXT PRIMARY KEY, Latitude FLOAT8,  Longitude FLOAT8, PM10 FLOAT8, PM25 FLOAT8)"
}


class DBConnection:
    def __init__(self,
                 dbname=os.environ['POSTGRES_DB'],
                 dbuser=os.environ['POSTGRES_USER'],
                 dbpassword=os.environ['POSTGRES_PASSWORD'],
                 hostname="database",
                 port=5432):
        #set up connection
        engineParams = f"postgresql+psycopg2://{dbuser}:{dbpassword}@{hostname}:{port}/{dbname}"
        exponentialBackoff = 1  #the number of seconds to wait between retrying connection
        while True:
            try:
                self._engine = create_engine(engineParams)
                self._conn = self._engine.raw_connection()
                self._cur = self._conn.cursor()
                break
            except (sqlalchemy.exc.OperationalError,
                    psycopg2.OperationalError):
                time.sleep(randint(0, exponentialBackoff))
                exponentialBackoff *= 2

    def createSensorTables(self):
        self._cur.execute(
            "CREATE TABLE IF NOT EXISTS position (uuid TEXT PRIMARY KEY, AccX FLOAT8, AccY FLOAT8 , AccZ FLOAT8, Acc_mag FLOAT8, Altitude FLOAT8, GyroX FLOAT8, GyroY FLOAT8, GyroZ FLOAT8, Gyro_mag FLOAT8, Latitude FLOAT8, Longitude FLOAT8, Speed FLOAT8)"
        )

        self._cur.execute(
            "CREATE TABLE IF NOT EXISTS weather(uuid TEXT PRIMARY KEY, DewPt FLOAT8, Humid FLOAT8, MxWSpd FLOAT8, Press FLOAT8, Rain FLOAT8, Sun FLOAT8, Temp_CBS FLOAT8, Temp_CL FLOAT8, WindDr FLOAT8, WindSp FLOAT8)"
        )

        for table, schema in Database.schemas.items():
        self._cur.execute(
                sql.SQL("CREATE TABLE IF NOT EXISTS {} {}").format(
                    sql.Identifier(table), sql.SQL(schema)))
        self._conn.commit()

        self._cur.execute(
            "CREATE TABLE IF NOT EXISTS system_status(uuid TEXT PRIMARY KEY, BatteryVIN FLOAT8, Satellites FLOAT8, gpsUpdated FLOAT8, nAcc FLOAT8)"
        )

        self._cur.execute(
            "CREATE TABLE IF NOT EXISTS air_quality (uuid TEXT PRIMARY KEY, Latitude FLOAT8,  Longitude FLOAT8, PM10 FLOAT8, PM25 FLOAT8)"
        )

        self._conn.commit()

    def insertSensorData(self, csvFile):
        data = pd.read_csv(csvFile)
        #generate unique uid for each measurement if not there
        if ("uuid" not in data.columns):
            data["uuid"] = [uuid.uuid4().hex for _ in range(data.shape[0])]
            data.to_csv(csvFile)
        for table in columns.keys():
            table_df = data[columns[table]]
            output = io.StringIO()
            table_df.to_csv(output, sep='\t', header=False, index=False)
            output.seek(0)
            self._cur.copy_from(output, table, null="")
        self._conn.commit()

    def insertAirPollutionData(self, data):
        if (len(data) % 4 != 0):  #should be lat, long, PM10, PM2.5
            return "Bad packets"
        for i in range(0, len(data), 4):
            self._cur.execute(
                "INSERT INTO air_quality (uuid, Latitude, Longitude, PM10, PM25) VALUES (%s, %s, %s, %s, %s)",
                (uuid.uuid4().hex, *list(data)[i:i + 4]))
        self._conn.commit()
        return "Successful insert"

    def queryAirPollution(self):
        self._cur.execute(
            "SELECT Latitude, Longitude, PM10, PM25 FROM air_quality;")
        return json.dumps(self._cur.fetchall())

    def getConnectionStats(self):
        return json.dumps(self._conn.get_dsn_parameters())

    def getDBInfo(self):  #print out all tables and their records
        tables = {}
        self._cur.execute(
            """SELECT table_name FROM information_schema.tables
       WHERE table_schema = 'public'"""
        )  # this gets an iterable collection of the public tables in the database
        for table in self._cur.fetchall():
            cur2 = self._conn.cursor()
            cur2.execute(
                sql.SQL("SELECT * FROM {} ;").format(sql.Identifier(table[0]))
            )  # note sql module used for safe dynamic SQL queries
            tables[table[0]] = cur2.fetchall()
        return json.dumps(tables)

    def clearData(self):
        self._cur.execute(
            """SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"""
        )  # this gets an iterable collection of the public tables in the database
        for table in self._cur.fetchall():
            cur2 = self._conn.cursor()
            cur2.execute(
                sql.SQL("DELETE FROM {} ;").format(sql.Identifier(table[0]))
            )  # note sql module used for safe dynamic SQL queries
        self._conn.commit()
