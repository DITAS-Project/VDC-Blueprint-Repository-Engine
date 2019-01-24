var hooks = require('hooks');
var before = hooks.before;
var after = hooks.after;

var responseStash = {};

after("/blueprints > POST > 201 > application/json", function (transaction) {

  // saving HTTP response to the stash
  responseStash[transaction.name] = transaction.real;
  console.log('responseStash');
});


before("/blueprints/{blueprint_id} > DELETE > 204", function (transaction) {
  //reusing data from previous response here
  var blueprintId = JSON.parse(responseStash['/blueprints > POST > 201 > application/json'])['id'];

  //replacing id in URL with stashed id from previous response
  var url = transaction.fullPath;
  transaction.fullPath = url.replace('5c49a32a763d081d264b20e0', blueprintId);
});