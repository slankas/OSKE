# Flask REST-based application to access Google Search Results

from flask import Flask,jsonify,make_response
import logging
import json
from googlesearch import search

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
logger = logging.getLogger('proxy')

app = Flask(__name__,static_folder=None)

@app.route('/')
def index():
    """Lists the available REST endpoints for the application.

    Args:
        none

    Returns:
        A json array containing a JSON array indexed at "endPoints".
        Each element in the array contains a JSON object with ...

        Example:
        {
          "code": 200,
          "endPoints": [
          {
            "methods": "GET,OPTIONS,HEAD",
            "rule": "/"
          },
          {
            "methods": "OPTIONS,POST",
            "rule": ""
          }
          ]
        }

    Raises:
        None
    """
    routes = []
    for rule in app.url_map.iter_rules():
        myRule = {}
        myRule["rule"] = rule.rule
        myRule["methods"] = ",".join(list(rule.methods))
        #myRule["function"] = rule.endpoint
        routes.append(myRule)

    return jsonify(code=200, endPoints=routes)

@app.route('/google/search/<searchTerm>/<numPages>',methods=['GET'])
def performGoogleSearch(searchTerm,numPages):
    """Search the google for the given terms and return the result

    Args:


    Returns:


    Raises:
        None.  all errors are captured. 401 returned for any raised exceptions.  Exception reason printed to log
    """

    try:
        search_results = search(searchTerm, num_results=int(numPages),advanced=True)
        return json.dumps([{"url": x.url,"title":x.title, "description":x.description} for x in search_results])

    except Exception as e:
        logger.warning (str(e))

        resp = make_response("",401)
        return resp


@app.errorhandler(404)
def not_found(error):
    return make_response(jsonify({'error': 'Not found'}), 404)

if __name__ == '__main__':
    app.run(debug=False,host='0.0.0.0',port=5000)
