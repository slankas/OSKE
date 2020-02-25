# Basic Flask application to provide access to various textrank functions
# through a REST API

from flask import Flask,abort,jsonify,make_response,request, url_for
import logging
import json
import datetime
import mf2py
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

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

@app.route('/microformat/v1/extract',methods=['POST'])
def getSummary():
    """ Extracts microformat data from the based in html data
        which is the body of the post data

    Args:
        post data - contents of the html file in which to extract
                    the microformat data.

    Returns:
        A json object

        Example:
        {
        }

    Raises:
        None
    """
    text = request.data.decode('utf-8')
    p = mf2py.Parser(doc=text)
    #parsedData = mf2py.parse(doc=text)
    return p.to_json()


@app.errorhandler(404)
def not_found(error):
    return make_response(jsonify({'error': 'Not found'}), 404)

if __name__ == '__main__':
    app.run(debug=True,host='0.0.0.0')
