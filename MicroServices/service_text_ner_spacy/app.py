# Flask REST-based application to retrieve named enties from a text string
# using the spaCy library

from flask import Flask,abort,jsonify,make_response,request, url_for
import logging
import json
import spacy

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
logger = logging.getLogger('proxy')

app = Flask(__name__,static_folder=None)
nlp = spacy.load('en_core_web_lg')


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

@app.route('/spacy/ner',methods=['POST'])
def performNamedEntityRecognition():
    """Perform named entity recognition of a text document using spacy

    Args:
      a json object should be passed in the body with a "text" attribute
		
    Returns:
      a json object with a single element "entities" that is an array of json objects with the following format
         text, startPos, endPost, type

    Raises:
        None.  all errors are captured. 401 returned for any raised exceptions.  Exception reason printed to log
    """

    try:
        text = request.json['text']
        showAll = False
        if ('showAll') in request.json:
            showAll = request.json['showAll']
        result = { 'entities': [] }
        doc = nlp(text)
        for ent in doc.ents:
            if showAll or not (ent.label_ in ['DATE','TIME','PERCENT','QUANTITY','ORDINAL','CARDINAL']):
                result['entities'].append({ 'text': ent.text, 'startPos': ent.start_char, 'endPos': ent.end_char, 'type': ent.label_ })

        return jsonify(result)
            
 

    except Exception as e:
        logger.warning (str(e))

        resp = make_response("",401)
        return resp


@app.errorhandler(404)
def not_found(error):
    return make_response(jsonify({'error': 'Not found'}), 404)

if __name__ == '__main__':
    app.run(debug=False,host='0.0.0.0',port=5000)
