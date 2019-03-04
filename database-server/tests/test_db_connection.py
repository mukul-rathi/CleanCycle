"""
This module runs all the tests for the DBConnection Class

"""

#standard imports
import json
import os

#third party imports
import pandas as pd
import numpy as np
import pytest

#local application imports
import app.src.db_connection as db_connection

#note that Pylint disable C0103 refes to disabling the "doesn't conform to snake_case" messages.


class TestDBConnection():
    """
    This class is responsible for testing the database connection interface.
    
    Attributes:
        _db: the database connection object

    Note all test_ methods have 
        Args: None
        Return: None
        Raises:
            AssertionError
    since these are all test methods.
    """
    _db = None

    @staticmethod
    def check_row_equality(row1, row2):
        """
            This function tests if two rows are equal, comparing float values within range of tolerance to account for rounding error.

            Args:
                row1: The first row to compare
                row2: the second row to compare against
            Returns:
                rows_are_equal: a boolean value (True if rows are equal)

        """
        rows_are_equal = len(row1) == len(row2)
        for i, row1_val in enumerate(row1):
            try:
                #if floats, check if within rounding error range
                rows_are_equal = rows_are_equal and np.isclose(
                    row1_val, row2[i])
            except TypeError:
                #not floats so compare as if strings
                rows_are_equal = rows_are_equal and str(row1_val) == str(
                    row2[i])
        return rows_are_equal

    def setup_method(self):
        """
        The setup method runs before every unit test.
        It establishes a fresh connection to the database and clears the data in database.
        This ensures unit tests have no shared state (independent of another).

        """
        self._db = db_connection.DBConnection()
        self._db.clear_data()  #clear data in database

    def teardown_method(self):
        """
        The teardown method runs after every unit test.
        It clears the data in database after the unit test runs.
        This ensures unit tests have no shared state (independent of another).

        """
        self._db.clear_data()

    def test_insert_backup_data(self):
        """
        This method tests if the insert back-up data does correctly insert the data in the CSV file.

        """
        self._db.insert_backup_data("test.csv")
        df = pd.read_csv("test.csv")  #pylint: disable=C0103
        tables = json.loads(self._db.get_database_info())
        for table, columns in db_connection.Database.get_columns().items():
            #check that each table has the corresponding records in csv
            for _, row in df[columns].iterrows():
                for record in tables[table]:
                    #find matching row in table
                    if row["uuid"] in record:
                        #check rest of fields in row match
                        assert TestDBConnection.check_row_equality(
                            list(record), list(row))

    def test_create_tables(self):
        """
        This method tests if all the tables in the database schema have been created.

        """
        self._db.create_tables()
        tables = json.loads(self._db.get_database_info())
        expected_tables = db_connection.Database.get_columns().keys()
        for table in expected_tables:
            assert table in tables.keys()

    def test_clear_data(self):
        """
        This method clears the database and iterates through tables to ensure no data left.

        """
        self._db.clear_data()
        tables = json.loads(self._db.get_database_info())
        for table in tables.keys():
            assert not tables[table]  #no values in database

    def test_insert_sensor_data(self):
        """
        This method tests if, given good sensor data the data is parsed correctly and inserted into the database correctly.

        """
        with open("test-sensor-data.json", "r") as f:  #pylint: disable=C0103
            sensor_data = list(json.load(f).get("payload_fields").values())
        self._db.insert_sensor_data(sensor_data)
        data_points = [
            sensor_data[i:i + 4] for i in range(0, len(sensor_data), 4)
        ]
        db_records = json.loads(self._db.query_air_pollution_data())
        for data_point in data_points:
            assert any([
                TestDBConnection.check_row_equality(data_point, record)
                for record in db_records
            ])  #check if any record matches data-point

    def test_insert_backup_data_without_uuid(self):
        """
        This method tests if the insert back-up data does correctly add a uuid and then  insert the data in the CSV file.

        """
        self._db.insert_backup_data("test.csv")
        df = pd.read_csv("test.csv")  #pylint: disable=C0103
        df = df.drop(columns=["uuid"])  #pylint: disable=C0103
        df.to_csv("test.csv")

        self.test_insert_backup_data()

    def test_connection_stats(self):
        """
        Checks if connected to correct database, as correct user on the correct port. 
        """
        connection_info = json.loads(self._db.get_connection_stats())

        #check connection consistent with environment variables
        assert connection_info["user"] == os.environ.get("POSTGRES_USER")
        assert connection_info["dbname"] == os.environ.get("POSTGRES_DB")
        assert connection_info["port"] == "5432"  #this is database port

    def test_insert_malformed_sensor_data(self):
        """
        This method tests if, given malformed sensor data 
        (where list of data fields is not groupable into groups of 4), 
        an IOError is raised. 

        """
        sensor_data = [1.1, 1.2]
        pytest.raises(IOError, self._db.insert_sensor_data, sensor_data)

    @staticmethod
    def test_bad_database_connection():
        """
        Tests if an IOError is raised after the max number of retries is exceeded when connecting to a database.

        Uses dependency injection (mocks a database address)
        """

        pytest.raises(
            IOError,
            db_connection.DBConnection,
            host_addr="fakehost:1000",
            max_num_tries=2)
