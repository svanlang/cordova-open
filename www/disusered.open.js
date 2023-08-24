/**
 * disusered.open.js
 *
 * @overview Open documents with compatible apps.
 * @author Carlos Antonio
 * @license MIT
*/

var exec = require('cordova/exec');

/**
 * open
 *
 * @param {String} uri File URI
 * @param {Function} success Success callback
 * @param {Function} error Failure callback
 * @param {Function} progress Callback for download progress reporting
 * @param {Boolean} trustAllCertificates Trusts any certificate when the connection is done over HTTPS.
 * @returns {void}
 */
exports.open = function(uri, success, error, progress, trustAllCertificates) {
  if (!uri || arguments.length === 0) { return false; }

  if (uri.match('http')) {
    downloadAndOpen(uri, success, error, progress, trustAllCertificates);
  } else {
    uri = encodeURI(uri);
    exec(onSuccess.bind(this, uri, success),
         onError.bind(this, error), 'Open', 'open', [uri]);
  }
};

/**
 * downloadAndOpen
 *
 * @param {String} url File URI
 * @param {Function} success Success callback
 * @param {Function} error Failure callback
 * @param {Function} progress Callback for download progress reporting
 * @param {Boolean} trustAllCertificates Trusts any certificate when the connection is done over HTTPS.
 * @returns {void}
 */
function downloadAndOpen(url, success, error, progress, trustAllCertificates) {
  var ft = new FileTransfer();
  var dir = cordova.file.cacheDirectory;
  var name = url.substring(url.lastIndexOf('/') + 1);
  var path = dir + name;

  if (typeof trustAllCertificates !== 'boolean') {
    // Defaults to false
    trustAllCertificates = false;
  }

  if (progress && typeof progress === 'function') {
    ft.onprogress = progress;
  }

  ft.download(url, path,
      function done(entry) {
        var file = entry.nativeURL;
        exec(onSuccess.bind(this, file, success),
             onError.bind(this, error), 'Open', 'open', [file]);
      },
      onError.bind(this, error),
      trustAllCertificates
  );
}

/**
 * onSuccess
 *
 * @param {String} path File URI
 * @param {Function} callback Callback
 * @param {type} type of success event
 * @returns {String} File URI
 */
function onSuccess(path, callback, type) {
  if(type !== 'resume') {
      fire('open.success', path);
      if (typeof callback === 'function') {
        callback(path);
      }
  } else {
      fire('resume', path);
  }
  return path;
}

/**
 * onError
 *
 * @param {Function} callback Callback
 * @returns {Number} Error Code
 */
function onError(callback) {
  var code = (arguments.length > 1) ? arguments[1] : 0;
  fire('open.error', code);
  if (typeof callback === 'function') {
    callback(code);
  }
  return code;
}

/**
 * fire
 *
 * @param {String} event Event name
 * @param {String} data Success or error data
 * @returns {void}
 */
function fire(event, data) {
  var channel = require('cordova/channel');
  var cordova = require('cordova');
  var payload = {};

  channel.onCordovaReady.subscribe(function() {
    var prop = (event === 'error') ? event : 'data';
    payload[prop] = data;
    cordova.fireDocumentEvent(event, payload);
  });
}
