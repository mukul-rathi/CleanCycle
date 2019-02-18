from flask import Flask
app = Flask(__name__)
from db_connection import DBConnection


@app.route('/', methods=['GET']) 
def connectionStats():
    db = DBConnection()
    return db.getConnectionStats()

@app.route('/analytics', methods=['GET']) 
def query():
    db = DBConnection()
    return db.queryAirPollution()

    db = DBConnection()
    db.insertItem()
    return "Inserted an item"

@app.route('/createTable', methods=['GET', 'POST']) 
def createTable():
    db = DBConnection()
    db.createTable()
    return "Created an table"


  
@app.route('/info', methods=['GET']) 
def test():
    db = DBConnection()
    return db.getDBInfo()
if __name__ == '__main__':
    app.run(debug=True,host='0.0.0.0', port=80)