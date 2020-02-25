var LASTextAnalysis = (function () {
"use strict";

var punctuationRegEx = /[.,!\?;:]/

var privateMembers = {
    punctuationRegEx : punctuationRegEx
};

function hasPunctuation(text) {
     return punctuationRegEx.test(text);
}

function getWords(text) {
    return text.match(/\S+/g);
}

function numWords(text) {
    var matches = text.match(/\S+/g) ;
    return matches ?matches.length:0;
}

function allWordsHaveUpperCaseLetter(text) {
    var words= text.match(/\S+/g)
    if (words === null) { return false;}
    var numWords = words.length;
    for (var i=0;i<numWords; i++) {
      if (words[i] === words[i].toLowerCase()) { return false; }

    }
    return true;
}

function eliminateNonSentences(text) {
  var lines = text.split(/\n/);
  var numLines = lines.length;
  var result = [];
  for (var i=0; i < numLines; i++) {
    var line = lines[i];
    if (numWords(line) > 0 && allWordsHaveUpperCaseLetter(line) === false) {
      result.push(line);
    }
  }
  return result.join('\n');
}

return {
    allWordsHaveUpperCaseLetter: allWordsHaveUpperCaseLetter,
    eliminateNonSentences : eliminateNonSentences,
    getWords : getWords,
    hasPunctuation: hasPunctuation,
    numWords: numWords,
    privateMembers : privateMembers
    };
}());

/*
module.exports = function() {
  this.LASTextAnalysis = LASTextAnalysis;
}
*/