"""
This module runs all the tests for the Endpoint

"""

#standard imports
import os
import json

#third party imports
import requests

#local application imports
from app.src import db_connection


class TestEndpoint():
    """
    This class is responsible for testing the endpoint.
    
    Attributes:
        endpoint: the base url of the endpoint

    Note all methods have 
        Args: None
        Return: None
        Raises:
            AssertionError
    since these are all test methods.

    The tests follow the format:
        Arrange
        Act
        Assert
    """
    endpoint = "http://endpoint"

    def test_connection(self):
        """
        Checks if connected to correct database, as correct user on the correct port. 
        """
        r = requests.get(url=self.endpoint + "/info")

        connection_info = r.json()

        #check connection consistent with environment variables
        assert connection_info["user"] == os.environ.get("POSTGRES_USER")
        assert connection_info["dbname"] == os.environ.get("POSTGRES_DB")
        assert connection_info["port"] == "5432"  #this is database port

    def test_analytics_endpoint(self):
        """
        Checks if the analytics data received is formatted correctly.
        Should be a list of records that each have 4 values (lat, long, pm10, pm2.5)
        """
        r = requests.get(url=(self.endpoint + "/analytics"))
        analytics = r.json()  #this returns a list of items
        for record in analytics:
            assert len(record) == 4

    def test_database_info_endpoint(self):
        """
        Checks if the database information received is formatted correctly.
        
        Should be a JSON object with table names as keys and list of records in each table as values. 
        Each of the list of records should be well-formatted - i.e. have the correct number of columns
        """
        ##
        expected_tables = db_connection.Database.get_columns().keys()

        r = requests.get(url=(self.endpoint))
        tables_info = r.json()

        for table in expected_tables:
            assert table in tables_info.keys()
            for record in tables_info.get(table):
                assert len(record) == len(
                    db_connection.Database.get_columns().get(table))

    def test_insert_good_sensor_data(self):
        """
        Checks if the endpoint processes the test-sensor-data JSON correctly and returns code 201.
        """
        with open("test-sensor-data.json", "r") as f:
            sensor_data = json.load(f)

        r = requests.post(
            url=(self.endpoint + "/insertSensorData"), json=sensor_data)

        assert r.status_code == 201

    def test_insert_sensor_data_missing_payload(self):
        """
        Checks if the endpoint handles a JSON with no payload fields correctly and returns code 400 without crashing.
        """
        sensor_data = {}

        r = requests.post(
            url=(self.endpoint + "/insertSensorData"), json=sensor_data)

        assert r.status_code == 400

    def test_insert_bad_sensor_data(self):
        """
        Checks if the endpoint handles JSON with badly formatted payload fields correctly and returns code 400 without crashing.
        """
        sensor_data = {
            "payload_fields": {
                "0": 1.0,
                "1": 1.1
            }
        }  #note not a multiple of 4

        r = requests.post(
            url=(self.endpoint + "/insertSensorData"), json=sensor_data)

        assert r.status_code == 400

    def test_insert_no_sensor_data(self):
        """
        Checks if the endpoint correctly handles a JSON with payload fields that are empty  and returns code 201 without crashing.
        """
        sensor_data = {"payload_fields": {}}

        r = requests.post(
            url=(self.endpoint + "/insertSensorData"), json=sensor_data)

        assert r.status_code == 201

    def test_insert_and_query_analytics_and_database(self):
        """
        Checks if after insertion the analytics and database info are in a consistent state.
        """
        with open("test-sensor-data.json", "r") as f:
            sensor_data = json.load(f)

        requests.post(
            url=(self.endpoint + "/insertSensorData"), json=sensor_data)
        self.test_analytics_endpoint()
        self.test_database_info_endpoint()
