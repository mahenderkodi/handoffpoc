import express, { Express } from 'express';

import { subscribeRouter } from './routes/subscribe';

export function createApp(): Express {
  const app = express();
  app.use(express.json());
  app.use(subscribeRouter);
  return app;
}
