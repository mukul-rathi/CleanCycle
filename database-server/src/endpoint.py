from flask import Flask, request
from flask_cors import CORS
from db_connection import DBConnection

app = Flask(__name__)
CORS(app)


@app.route('/', methods=['GET'])
def connectionStats():
    db = DBConnection()
    return db.getConnectionStats()


    try:
        db = db_connection.DBConnection()
    except IOError:
        return "Database connection not possible", 504, {'ContentType':'text/plain'}
    return db.get_database_info(), 200, {'ContentType':'application/json'}
    
    try:
        db = db_connection.DBConnection()
    except IOError:
        return "Database connection not possible", 504, {'ContentType':'text/plain'}
    return db.get_connection_stats(), 200, {'ContentType':'application/json'}

@app.route('/analytics', methods=['GET'])
    try:
        db = db_connection.DBConnection()
    except IOError:
        return "Database connection not possible", 504, {'ContentType':'text/plain'}
    return db.query_air_pollution_data(), 200, {'ContentType':'application/json'}



    try:
        db = db_connection.DBConnection()
    except IOError:
        return "Database connection not possible", 504, {'ContentType':'text/plain'}

    sensor_data = request.get_json().get(
        "payload_fields")  #this contains the air pollution data
    if sensor_data: #i.e. we have the payload fields 
        try:
            db.insert_sensor_data(sensor_data.values())
            return "Successful Insertion", 201, {'ContentType':'text/plain'}

        except IOError:
            return "Error: malformed payload fields data", 400, {'ContentType':'text/plain'}

    else:
        return "Error no payload fields in JSON", 400, {'ContentType':'text/plain'}



def runEndpoint():
    app.run(debug=True, host='0.0.0.0', port=80)
