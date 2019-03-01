from flask import Flask, request
from flask_cors import CORS
from db_connection import DBConnection


app = Flask(__name__)
CORS(app)

@app.route('/', methods=['GET']) 
def connectionStats():
    db = DBConnection()
    return db.getConnectionStats()

@app.route('/analytics', methods=['GET']) 
def query():
    db = DBConnection()
    return db.queryAirPollution()

@app.route('/insertSensorData', methods=['GET', 'POST']) 
def insertSensorData():
 
   db = DBConnection()
   if (request.method == 'POST'):
      sensorData = request.get_json().get("payload_fields").values() #this contains the air pollution data
      if(sensorData):
         return db.insertAirPollutionData(sensorData)
            
   return "Hello"  
@app.route('/info', methods=['GET']) 
def test():
    db = DBConnection()
    return db.getDBInfo()


def runEndpoint():
    app.run(debug=True,host='0.0.0.0', port=80)