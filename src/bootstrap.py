"""
This module contains the code to run the bootstrap procedure for the endpoint
 and database.

"""

#standard imports
import glob

#local application imports -``
try:
    import db_connection
    import endpoint
except ModuleNotFoundError:
    from app.src import db_connection
    from app.src import endpoint


def bootstrap():
    """
        Sets up the database with a clean consistent state. 
        It creates the tables, clears any existing data and restores data from 
        backup csvs.

        Args: None
           
        Returns: None
    """
    print("Beginning bootstrap procedure...")  #for debugging
    db = db_connection.DBConnection()  #pylint: disable=C0103
    print("Established connection to db...")  #for debugging

    db.create_tables()
    print("Created database tables")  #for debugging

    #clear any lingering data and start afresh
    db.clear_data()

    # iterate through backup csv folder and add them to database
    for csv_file in glob.glob('/usr/backups/*.csv'):
        db.insert_backup_data(csv_file)
    print("Bootstrap procedure complete")  #for debugging


if __name__ == '__main__':  #set up database and endpoint
    bootstrap()
    endpoint.run()
