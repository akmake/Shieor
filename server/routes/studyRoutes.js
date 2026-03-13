import express from 'express';
import { getDailyStudy } from '../controllers/studyController.js';

const router = express.Router();

router.get('/day', getDailyStudy);

export default router;
