'use strict';

const express = require('express');
const { createRouter } = require('./routes');
const { PORT } = require('./config');

const app = express();
app.disable('x-powered-by');
app.use(createRouter());

const server = app.listen(PORT, '0.0.0.0', () => {
  console.log(`[bridgetalk-backend] listening on http://localhost:${PORT}`);
});

function shutdown(signal) {
  console.log(`[bridgetalk-backend] received ${signal}, shutting down`);
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(1), 5000).unref();
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));

module.exports = { app, server };
