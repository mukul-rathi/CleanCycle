import pytest
import requests
import os
import json


class TestEndpoint(object):
    endpoint = "http://endpoint"

    def test_connection(self):
        r = requests.get(url=self.endpoint)
        connection_info = r.json()
        #check connection consistent with environment variables
        assert (connection_info["user"] == os.environ.get("POSTGRES_USER"))
        assert (connection_info["dbname"] == os.environ.get("POSTGRES_DB"))
        assert (connection_info["port"] == "5432")  #this is database port

    def test_analytics_endpoint(self):
        r = requests.get(url=(self.endpoint + "/analytics"))
        analytics = r.json()  #this returns a list of items
        for record in analytics:
            #each record should have 4 values (lat, long, pm10, pm2.5)
            assert len(record) == 4

    def test_db_info_endpoint(self):
        r = requests.get(url=(self.endpoint + "/info"))
        tablesInfo = r.json()
        #this should return a json object with the table names as keys and list of records in each table as values
        expectedTables = [
            "position", "weather", "time", "system_status", "air_quality"
        ]
        for table in expectedTables:
            assert table in tablesInfo.keys()

    def test_insert_sensor_data(self):
        with open("test-sensor-data.json", "r") as f:
    def test_insert_no_sensor_data(self):
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
        sensor_data = {"payload_fields":{
            "0": 1.0,
            "1": 1.1
            }} #note not a multiple of 4
        r = requests.post(
            url=(self.endpoint + "/insertSensorData"), json=sensor_data)
        assert r.status_code == 400
