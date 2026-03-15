import express from 'express';
import dotenv from 'dotenv';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import studyRoutes from './routes/studyRoutes.js';
import globalErrorHandler from './middlewares/errorMiddleware.js';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;

// הגדרות נתיבים עבור קבצים סטטיים (בגלל השימוש ב-ES Modules)
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

app.use(
  cors({
    origin: ['http://localhost:5173', process.env.CLIENT_URL].filter(Boolean),
    credentials: true,
  })
);
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// --- API Routes ---
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.use('/api/study', studyRoutes);

// טיפול בבקשות API לכתובות שלא קיימות (חובה לפני ה-Catch-all של ה-React)
app.use('/api/*', (req, res) => {
  res.status(404).json({ message: 'API endpoint not found' });
});

// --- Frontend Routes ---
// הגשת הקבצים הסטטיים של ה-React מתוך תיקיית dist
const clientDistPath = path.join(__dirname, '../client/dist');
app.use(express.static(clientDistPath));

// Catch-all: כל בקשה אחרת שלא טופלה למעלה, תחזיר את אתר ה-React
app.get('*', (req, res) => {
  const filePath = path.join(clientDistPath, 'index.html');
  res.sendFile(filePath, (err) => {
    if (err) {
      res.status(500).send('Error loading the application.');
    }
  });
});

// טיפול גלובלי בשגיאות השרת
app.use(globalErrorHandler);

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});