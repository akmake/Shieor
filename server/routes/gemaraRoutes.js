import express from 'express';
import multer from 'multer';
import { extractGemara, getPages, getTractates, updatePage } from '../controllers/gemaraController.js';

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 500 * 1024 * 1024 } });

router.post('/extract', upload.single('pdf'), extractGemara);
router.get('/tractates', getTractates);
router.get('/pages/:tractate', getPages);
router.put('/pages/:id', updatePage);

export default router;
