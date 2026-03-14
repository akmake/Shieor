import express from 'express';
import dotenv from 'dotenv';
import cors from 'cors';
import studyRoutes from './routes/studyRoutes.js';
import globalErrorHandler from './middlewares/errorMiddleware.js';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;

app.use(
  cors({
    origin: ['http://localhost:5173', process.env.CLIENT_URL].filter(Boolean),
    credentials: true,
  })
);
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.use('/api/study', studyRoutes);

app.get('/', (req, res) => {
  res.send('Shieor API is running');
});

app.use('*', (req, res) => {
  res.status(404).json({ message: 'API endpoint not found' });
});

app.use(globalErrorHandler);

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
