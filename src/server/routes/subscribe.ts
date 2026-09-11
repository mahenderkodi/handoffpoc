import { Router, Request, Response } from 'express';

import { subscribe } from '../services/subscriptionService';

export const subscribeRouter = Router();

interface SubscribeBody {
  email?: unknown;
}

subscribeRouter.post('/api/subscribe', (req: Request<unknown, unknown, SubscribeBody>, res: Response) => {
  const { email } = req.body;

  if (typeof email !== 'string' || email.trim().length === 0) {
    return res.status(400).json({ message: 'email is required' });
  }

  const result = subscribe(email);

  switch (result.status) {
    case 'subscribed':
      return res.status(201).json({ message: 'Subscribed successfully', email: result.email });
    case 'duplicate':
      return res.status(409).json({ message: 'This email is already subscribed', email: result.email });
    case 'invalid':
      return res.status(400).json({ message: 'Please provide a valid email address', email: result.email });
  }
});
