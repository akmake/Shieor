global.DOMMatrix = class DOMMatrix {
  constructor() { this.a=1;this.b=0;this.c=0;this.d=1;this.e=0;this.f=0; }
  multiply() { return this; }
  translate() { return this; }
  scale() { return this; }
};
global.ImageData = class ImageData {
  constructor(w,h) { this.width=w;this.height=h;this.data=new Uint8ClampedArray(w*h*4); }
};
global.Path2D = class Path2D {};

async function extractText(buffer) {
  const pdfjsLib = await import('pdfjs-dist/legacy/build/pdf.mjs');
  const pdf = await pdfjsLib.getDocument({ data: new Uint8Array(buffer) }).promise;
  const lines = [];
  for (let i = 1; i <= pdf.numPages; i++) {
    const page = await pdf.getPage(i);
    const content = await page.getTextContent();
    lines.push(content.items.map(item => item.str).join(''));
  }
  return { text: lines.join('\n'), numPages: pdf.numPages };
}

module.exports = { extractText };
