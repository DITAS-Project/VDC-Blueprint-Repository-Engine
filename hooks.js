var hooks = require('hooks');
var before = hooks.before;
var after = hooks.after;

var responseStash = {};

after("/blueprints > POST > 201 > application/json", function (transaction) {
  responseStash[transaction.name] = transaction.real.body;
});

before("/blueprints/{blueprint_id} > GET > 200", function (transaction) {
  var blueprintId = JSON.parse(responseStash['/blueprints > POST > 201 > application/json'])['blueprint_id'];
  var url = transaction.fullPath;
  transaction.fullPath = url.replace('5c49caf3763d081d264b2637', blueprintId);
});

before("/blueprints/{blueprint_id} > PATCH > 200", function (transaction) {
  var blueprintId = JSON.parse(responseStash['/blueprints > POST > 201 > application/json'])['blueprint_id'];
  var url = transaction.fullPath;
  transaction.fullPath = url.replace('5c49caf3763d081d264b2637', blueprintId);
});

before("/blueprints/{blueprint_id} > DELETE > 204", function (transaction) {
  var blueprintId = JSON.parse(responseStash['/blueprints > POST > 201 > application/json'])['blueprint_id'];
  var url = transaction.fullPath;
  transaction.fullPath = url.replace('5c49caf3763d081d264b2637', blueprintId);
});
