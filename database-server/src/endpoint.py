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
    return db.queryAll()
@app.route('/sensors', methods=['GET', 'POST']) 
def insert():
    db = DBConnection()
    db.insertItem()
    return "Inserted an item"

@app.route('/createTable', methods=['GET', 'POST']) 
def createTable():
    db = DBConnection()
    db.createTable()
    return "Created an table"


  
if __name__ == '__main__':
    app.run(debug=True,host='0.0.0.0', port=80)