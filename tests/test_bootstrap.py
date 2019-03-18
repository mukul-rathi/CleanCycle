"""
This module runs all the tests for the Bootstrap Class

"""

#standard imports
import shutil
import glob
import json
#third party imports

#local application imports
from app.src import db_connection
from app.src import bootstrap


class TestBootstrap():
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
    def copy_csv_files(src_path, dest_path):
        """
        This moves all CSV in the src folder to the dest folder
        Args:
            src_path: the path of the src folder
            dest_path: the path fo the dest folder

            Note both args must have a trailing /
        Returns: None 
        """
        for csv_file_path in glob.glob(src_path + '*.csv'):
            file_name = csv_file_path.split('/')[-1]
            shutil.copyfile(csv_file_path, dest_path + file_name)

    def setup_method(self):
        """
        The setup method runs before every unit test.
        It establishes a fresh start for the bootstrap procedure, clearing the 
        data.
        The backup CSV files are also cleared ensuring the unit tests can 
        control which files are present during the bootstrap procedure.

        """
        self._db = db_connection.DBConnection()
        self._db.clear_data()  #clear data in database

    # shutil.rmtree('/usr/backups/')

    def teardown_method(self):
        """
        The teardown method runs after every unit test.
        It clears the data in database after the unit test runs.
        This ensures the bootstrap unit tests have no shared state 
        (independent of another).

        """
        self._db.clear_data()

    def test_bootstrap_no_files(self):
        """
        This tests whether bootstrap creates the requisite tables, and that ]
        they have nothing in them.

        """
        expected_tables = db_connection.Database.get_columns().keys()

        bootstrap.bootstrap()

        actual_tables = json.loads(self._db.get_database_info())

        for table in expected_tables:
            assert table in actual_tables

    def test_bootstrap_with_files(self):
        """
        This tests whether bootstrap copies the data from the backup files 
        into the database correctly

        """
        TestBootstrap.copy_csv_files('/usr/src/app/src/backups/',
                                     '/usr/backups/')
        expected_tables = db_connection.Database.get_columns().keys()

        bootstrap.bootstrap()

        actual_tables = json.loads(self._db.get_database_info())

        for table in expected_tables:
            assert table in actual_tables
