import Article from '../models/Article.js';

export const uploadArticle = async (req, res, next) => {
  try {
    if (!req.file) {
      return res.status(400).json({ message: 'לא הועלה קובץ PDF' });
    }

    const title = req.body.title?.trim() || req.file.originalname.replace(/\.pdf$/i, '');
    const rawText = req.body.rawText?.trim() || '';
    const pageCount = parseInt(req.body.pageCount) || 0;

    if (!rawText) {
      return res.status(422).json({ message: 'לא התקבל טקסט מהקובץ' });
    }

    const article = await Article.create({
      title,
      originalFilename: req.file.originalname,
      rawText,
      pageCount,
    });

    res.status(201).json(article);
  } catch (err) {
    next(err);
  }
};

export const getArticles = async (req, res, next) => {
  try {
    const articles = await Article.find({}, '-rawText').sort({ createdAt: -1 });
    res.json(articles);
  } catch (err) {
    next(err);
  }
};

export const getArticleById = async (req, res, next) => {
  try {
    const article = await Article.findById(req.params.id);
    if (!article) return res.status(404).json({ message: 'מאמר לא נמצא' });
    res.json(article);
  } catch (err) {
    next(err);
  }
};

export const deleteArticle = async (req, res, next) => {
  try {
    const article = await Article.findByIdAndDelete(req.params.id);
    if (!article) return res.status(404).json({ message: 'מאמר לא נמצא' });
    res.json({ message: 'נמחק בהצלחה' });
  } catch (err) {
    next(err);
  }
};
export const updateArticle = async (req, res, next) => {
  try {
    const { title, rawText } = req.body;
    const article = await Article.findByIdAndUpdate(
      req.params.id,
      { title, rawText },
      { new: true }
    );
    if (!article) return res.status(404).json({ message: 'המאמר לא נמצא' });
    res.json(article);
  } catch (err) {
    next(err);
  }
};