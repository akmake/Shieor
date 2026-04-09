import mongoose from 'mongoose';

const sectionSchema = new mongoose.Schema({
  type: { type: String, enum: ['משנה', 'גמרא', 'פרק', 'הדרן'], required: true },
  text: { type: String, required: true },
}, { _id: false });

const gemaraPageSchema = new mongoose.Schema({
  tractate:  { type: String, required: true },  // מסכת
  bookFile:  { type: String, required: true },  // שם קובץ המקור
  pageNum:   { type: Number, required: true },  // מספר דף ב-PDF
  dafRef:    { type: String },                  // "סוטה ג:א"
  amud:      { type: String, enum: ['א', 'ב'] }, // עמוד א או ב
  perekName: { type: String },                  // "פרק ראשון" / "פרק שני"
  sections:  { type: [sectionSchema], default: [] }, // חלקי הדף המובנים
  text:      { type: String, required: true },  // טקסט מלא (לחיפוש)
  createdAt: { type: Date, default: Date.now },
});

gemaraPageSchema.index({ tractate: 1, pageNum: 1 });
gemaraPageSchema.index({ tractate: 1, dafRef: 1 });

export default mongoose.model('GemaraPage', gemaraPageSchema);
