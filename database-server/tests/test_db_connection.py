import pytest
from src_code.db_connection import DBConnection, columns as tableSchema
import json
import pandas as pd
import numpy as np


class TestDBConnection():
    _db = None

    #set up method run before each unit test
    def setup_method(self):
        self._db = DBConnection()  #establish connection
        self._db.clearData()  #clear data in database

    def teardown_method(self):
        self._db.clearData()  #clear data in database

    #note that database performs some rounding when importing in float data - so we need to check this explicitly
    def checkRowEquality(self, row1, row2):
        rowsEqual = True
        for i in range(len(row1)):
            try:
                #if floats, check if within rounding error range
                rowsEqual = rowsEqual and np.isclose(row1[i], row2[i])
            except TypeError:
                rowsEqual = rowsEqual and str(row1[i]) == str(row2[i])
        return rowsEqual

    def test_insertSensorData(self):
        self._db.insertSensorData("test.csv")
        df = pd.read_csv("test.csv")
        tables = json.loads(self._db.getDBInfo())
        for table, schema in tableSchema.items():
            #check that each table has the corresponding records in csv
            for _, row in df[schema].iterrows():
                for record in tables[table]:
                    #find matching row in table
                    if row["uuid"] in record:
                        #check rest of fields in row match
                        assert self.checkRowEquality(list(record), list(row))

    def test_createSensorTables(self):
        self._db.createSensorTables()
        tables = json.loads(self._db.getDBInfo())
        expectedTables = tableSchema.keys()
        for table in expectedTables:
            assert table in tables.keys()

    def test_clearData(self):
        self._db.clearData()
        tables = json.loads(self._db.getDBInfo())
        for table in tables.keys():
            assert len(tables[table]) == 0  #no values in database

    def test_insertAirPollutionData(self):
        with open("test-sensor-data.json", "r") as f:
            sensorData = list(json.load(f).get("payload_fields").values())
        self._db.insertAirPollutionData(sensorData)
        dataPoints = [
            sensorData[i:i + 4] for i in range(0, len(sensorData), 4)
        ]
        dbrecords = json.loads(self._db.queryAirPollution())
        for dataPoint in dataPoints:
            assert any([
                self.checkRowEquality(dataPoint, record)
                for record in dbrecords
            ])  #check if any record matches data-point
