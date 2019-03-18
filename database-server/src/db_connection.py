"""
This module contains all the database information.
There is a Database class to define the schema of the database.
The DBConnection class provides a clean interface to perform operations
on the database.

"""
#standard imports
import json
import os
import uuid
import io
import time
import random

#third party imports
import psycopg2
from psycopg2 import sql
import sqlalchemy
from sqlalchemy import create_engine
import pandas as pd


class Database:
    """
    This class is responsible for defining the PostgreSQL database schema.
    
    Attributes:
        columns: dictionary mapping table names to column names
        schemas: dictionary mapping table names to schema
    """
    _columns = {
        "position": [
            "uuid", "AccX", "AccY", "AccZ", "Acc_mag", "Altitude", "GyroX",
            "GyroY", "GyroZ", "Gyro_mag", "Latitude", "Longitude", "Speed"
        ],
        "weather": [
            "uuid", "DewPt", "Humid", "MxWSpd", "Press", "Rain", "Sun",
            "Temp_CBS", "Temp_CL", "WindDr", "WindSp"
        ],
        "time": ["uuid", "Date", "Counter", "Millis", "Start", "Time"],
        "system_status":
        ["uuid", "BatteryVIN", "Satellites", "gpsUpdated", "nAcc"],
        "air_quality": ["uuid", "Latitude", "Longitude", "PM10", "PM2.5"]
    }

    _schemas = {
        "position":
        "(uuid TEXT PRIMARY KEY, AccX FLOAT8, AccY FLOAT8 , AccZ FLOAT8,\
        Acc_mag FLOAT8, Altitude FLOAT8, GyroX FLOAT8, GyroY FLOAT8, GyroZ \
        FLOAT8, Gyro_mag FLOAT8, Latitude FLOAT8, Longitude FLOAT8, Speed \
        FLOAT8)",
        "weather":
        "(uuid TEXT PRIMARY KEY, DewPt FLOAT8, Humid FLOAT8, MxWSpd FLOAT8, \
        Press FLOAT8, Rain FLOAT8, Sun FLOAT8, Temp_CBS FLOAT8, \
        Temp_CL FLOAT8, WindDr FLOAT8, WindSp FLOAT8)",
        "time":
        "(uuid TEXT PRIMARY KEY, Date FLOAT8, Counter FLOAT8,Millis FLOAT8, \
        Start TEXT, Time FLOAT8)",
        "system_status":
        " (uuid TEXT PRIMARY KEY, BatteryVIN FLOAT8, Satellites FLOAT8, \
        gpsUpdated FLOAT8, nAcc FLOAT8)",
        "air_quality":
        "(uuid TEXT PRIMARY KEY, Latitude FLOAT8,  Longitude FLOAT8, \
        PM10 FLOAT8, PM2_5 FLOAT8)"
    }

    @classmethod
    def get_columns(cls):
        """ 
        Getter method for columns
        """
        return cls._columns

    @classmethod
    def get_schemas(cls):
        """ 
        Getter method for schemas
        """
        return cls._schemas


class DBConnection:
    """
    This class is responsible for initiating the connection with the 
    PostgreSQL database.
    It provides a clean interface to perform operations on the database.
    
    Attributes:
        _engine: API object used to interact with database.
        _conn: handles connection (encapsulates DB session)
        _cur:  cursor object to execute PostgreSQl commands
    """

    def __init__(self,
                 db_user=os.environ['POSTGRES_USER'],
                 db_password=os.environ['POSTGRES_PASSWORD'],
                 host_addr="database:5432",
                 max_num_tries=20):
        """
        Initiates a connection with the PostgreSQL database as the given user 
        on the given port.

        This tries to connect to the database, if not it will retry with 
        increasing waits in between.
      

        Args:
            db_user: the name of the user connecting to the database.
            db_password: the password of said user.
            host_addr: (of the form <host>:<port>) the address where the 
            database is hosted 
            For the Postgres docker container, the default port is 5432 and 
            the host is "database".
            Docker resolves "database" to the internal subnet URL of the 
            database container.
            max_num_tries: the maximum number of tries the __init__ method 
            should try to connect to the database for.
        Returns: None (since __init__)
        Raises:
            IOError: An error occurred accessing the database.
            Raised if after the max number of tries the connection still hasn't
            been established.
        """
        db_name = os.environ['POSTGRES_DB']

        engine_params = (f'postgresql+psycopg2://{db_user}:{db_password}@'
                         f'{host_addr}/{db_name}')
        num_tries = 1

        while True:
            try:
                self._engine = create_engine(engine_params)
                self._conn = self._engine.raw_connection()
                self._cur = self._conn.cursor()
                break
            except (sqlalchemy.exc.OperationalError,
                    psycopg2.OperationalError):
                # Use binary exponential backoff
                #- i.e. sample a wait between [0..2^n]
                #when n = number of tries.
                time.sleep(random.randint(0, 2**num_tries))
                if num_tries > max_num_tries:
                    raise IOError("Database unavailable")
                num_tries += 1

    def create_tables(self):
        """
        Creates the database tables based on schema definition in Database 
        class.

        Args: None
           
        Returns: None (since commits execution result to database)
        """
        for table, schema in Database.get_schemas().items():
            self._cur.execute(
                sql.SQL("CREATE TABLE IF NOT EXISTS {} {}").format(
                    sql.Identifier(table), sql.SQL(schema)))
        self._conn.commit()

    def insert_backup_data(self, csv_file):
        """
        Inserts the database backup CSV data. 
        If a unique uid for each measurement is not present, it generates one 
        and stores it in the CSV file.

        The rows in the CSV are then inserted as records in the respective 
        tables.

        Args: 
            csv_file: the path of the CSV file
           
        Returns: None (since commits data to database)
        """
        data = pd.read_csv(csv_file)
        if "uuid" not in data.columns:
            data["uuid"] = [uuid.uuid4().hex for _ in range(data.shape[0])]
            data.to_csv(csv_file)

    #CSV for each table converted to a string and copied across to database.
    #Alternative could be to do Batch Insert
        for table in Database.get_columns():
            table_df = data[Database.get_columns()[table]]
            output = io.StringIO()
            table_df.to_csv(output, sep='\t', header=False, index=False)
            output.seek(0)
            self._cur.copy_from(output, table, null="")
        self._conn.commit()

    def insert_sensor_data(self, data):
        """
        Inserts the air pollution sensor data into database

        Args: 
            data: a list of data values from the hardware. 
            This is logically grouped into groups of 4 values. 
            Each group is a measurement of (lat, long, PM10, PM2.5).
           
        Returns: None (since commits data to database)

        Raises:
            IOError: if data malformed (i.e. not a multiple of 4, so missing 
            values)
        """
        if len(data) % 4 != 0:
            raise IOError("Malformed packet data")

        for i in range(0, len(data), 4):
            self._cur.execute(
                "INSERT INTO air_quality (uuid, Latitude, Longitude, PM10,\
                 PM2_5) VALUES (%s, %s, %s, %s, %s)",
                (uuid.uuid4().hex, *list(data)[i:i + 4]))
        self._conn.commit()

    def query_air_pollution_data(self):
        """
        Returns the air pollution sensor data from the database.
        This is used for analytics.

        Args: None
           
        Returns: List of (lists of 4 values) - this corresponds to records of 
        (Lat, Long, PM10, PM2.5).
        """
        self._cur.execute(
            "SELECT Latitude, Longitude, PM10, PM2_5 FROM air_quality;")
        return json.dumps(self._cur.fetchall())

    def get_connection_stats(self):
        """
       Returns the statistics for the database connection (useful for 
       debugging). 

        Args: None
           
        Returns: A JSON string consisting of connection statistics. 
        e.g. 
        {"user": "tester", "dbname": "testdatabase",
         "host": "database", "port": "5432", 
         "tty": "", "options": "", "sslmode": "prefer", 
         "sslcompression": "0", "krbsrvname": "postgres",
         "target_session_attrs": "any"}
         """
        return json.dumps(self._conn.get_dsn_parameters())

    def get_database_info(self):
        """
        Returns the data stored in the database, indexed by table.

        Args: None
           
        Returns: A JSON string where keys are table names and values are lists 
        of lists.
        This corresponds to the list of records in that table.
        """
        tables = {}
        self._cur.execute(
            "SELECT table_name FROM information_schema.tables \
       WHERE table_schema = 'public'"
        )  # returns an iterable collection of public tables in the database
        for table in self._cur.fetchall():
            cur2 = self._conn.cursor()
            cur2.execute(
                sql.SQL("SELECT * FROM {} ;").format(sql.Identifier(table[0]))
            )  # note sql module used for safe dynamic SQL queries
            tables[table[0]] = cur2.fetchall()
        return json.dumps(tables)

    def clear_data(self):
        """
        Clears the data stored in the database.

        This is useful for bootstrap and unit tests that want to start with a 
        fresh state.

        Args: None
           
        Returns: None
        """
        self._cur.execute(
            "SELECT table_name FROM information_schema.tables WHERE \
            table_schema = 'public'"
        )  # returns an iterable collection of public tables in the database
        for table in self._cur.fetchall():
            cur2 = self._conn.cursor()
            cur2.execute(
                sql.SQL("DELETE FROM {} ;").format(sql.Identifier(table[0]))
            )  # note sql module used for safe dynamic SQL queries
        self._conn.commit()
