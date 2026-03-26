import express from 'express';
import multer from 'multer';
import { uploadArticle, getArticles, getArticleById, deleteArticle } from '../controllers/articleController.js';

const router = express.Router();

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 20 * 1024 * 1024 }, // 20MB
  fileFilter: (req, file, cb) => {
    if (file.mimetype === 'application/pdf') cb(null, true);
    else cb(new Error('Only PDF files are allowed'));
  },
});

router.post('/upload', upload.single('pdf'), uploadArticle);
router.get('/', getArticles);
router.get('/:id', getArticleById);
router.delete('/:id', deleteArticle);

export default router;
