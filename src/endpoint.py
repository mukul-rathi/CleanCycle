"""
This module contains the implementation of the endpoint.

The endpoint provides a clean REST API for other sections of the project to 
query and modify the database.

"""
#third party imports
from flask import Flask, request
from flask_cors import CORS

#local application imports

try:
    import db_connection
except ModuleNotFoundError:
    from app.src import db_connection

app = Flask(__name__)  #pylint: disable=C0103
CORS(app)


@app.route('/', methods=['GET'])
def database_info():
    """
        Returns the all data stored in the database.

        Args: None
           
        Returns: 
            Response 200 and JSON String where values are list of records 
            indexed by table name as key. 
            Response 504 if database connection not possible


    """
    try:
        db = db_connection.DBConnection()
    except IOError:
        return "Database connection not possible", 504, {
            'ContentType': 'text/plain'
        }
    return db.get_database_info(), 200, {'ContentType': 'application/json'}


@app.route('/info', methods=['GET'])
def connection_stats():
    """
        Returns the connection stats for database connection (used for 
        debugging).

        Args: None
           
        Returns: 
            Response 200 and JSON String of connection stats - see 
            DBConnection class for more info. 
            
            Response 504 if database connection not possible

    """
    try:
        db = db_connection.DBConnection()
    except IOError:
        return "Database connection not possible", 504, {
            'ContentType': 'text/plain'
        }
    return db.get_connection_stats(), 200, {'ContentType': 'application/json'}


@app.route('/analytics', methods=['GET'])
def query_air_pollution_data():
    """
        Returns the air pollution data
        Args: None
           
        Returns: 
            Response 200 and JSON String - List of records 
            (Lat, Long, PM10, PM2.5).
                Again see DBConnection class for more info. 
            
            Response 504 if database connection not possible

    """
    try:
        db = db_connection.DBConnection()
    except IOError:
        return "Database connection not possible", 504, {
            'ContentType': 'text/plain'
        }
    return db.query_air_pollution_data(), 200, {
        'ContentType': 'application/json'
    }


@app.route('/insertSensorData', methods=['POST'])
def insert_sensor_data():
    """
        Takes a post request with a JSON object of form
        {
        "app_id": "cleancycle-application",
        "dev_id": "",
        "hardware_serial": "",
        "port": 1,
        "counter": 0,
        "payload_raw": "",
        "payload_fields": {
            "0": 52.19398498535156,
            "1": 0.1360626220703125,
            "2": 38,
            "3": 29,
            "4": 52.19419860839844,
            ....
        },
        "metadata": {
            "time": "2019-02-25T16:00:46.840341614Z"
        },
        "downlink_url": ""
        }

        Payload fields:
        JSON object where keys 0,1,2,3 correspond to 
        (Lat, Long, PM10, PM2.5) for first measurement.
        Subsequent measurements can be obtained by grouping subsequent keys 
        by 4
     

    Args: None

    Returns:
        Response 201 if data inserted successfully
        Response 400 iif data has no payload.
        Response 400 if data payload_fields are malformed - i.e. can't be 
        grouped as records
        Response 504 if database connection not possible

    """
    try:
        db = db_connection.DBConnection()
    except IOError:
        return "Database connection not possible", 504, {
            'ContentType': 'text/plain'
        }

    #this contains the air pollution data
    if "payload_fields" in request.get_json(
    ):  #i.e. we have the payload fields
        sensor_data = request.get_json().get("payload_fields")
        try:
            db.insert_sensor_data(sensor_data.values())
            return "Successful Insertion", 201, {'ContentType': 'text/plain'}

        except IOError:
            return "Error: malformed payload fields data", 400, {
                'ContentType': 'text/plain'
            }

    else:
        return "Error no payload fields in JSON", 400, {
            'ContentType': 'text/plain'
        }


def run():
    """
    Start up Flask endpoint on port 80
    """
    app.run(debug=True, host='0.0.0.0', port=80)
