const __vite__mapDeps=(i,m=__vite__mapDeps,d=(m.f||(m.f=["assets/ElectricalCADPage-BFDNCOFE.js","assets/pdf-vendor-BcAT4u7F.js","assets/react-vendor-BSBmiyrG.js","assets/file-vendor-C6jS1QLd.js","assets/electricalEngine-DlBWHuBz.js","assets/three-vendor-b8aiInxg.js","assets/FloorPlanStudioPage-BJI6KElA.js"])))=>i.map(i=>d[i]);
import{r as i,j as e,L as f,N as x,O as _,e as k,f as u,g as L,R as A,B as P,F as R}from"./react-vendor-BSBmiyrG.js";import{_ as j}from"./pdf-vendor-BcAT4u7F.js";import"./file-vendor-C6jS1QLd.js";(function(){const s=document.createElement("link").relList;if(s&&s.supports&&s.supports("modulepreload"))return;for(const a of document.querySelectorAll('link[rel="modulepreload"]'))o(a);new MutationObserver(a=>{for(const r of a)if(r.type==="childList")for(const n of r.addedNodes)n.tagName==="LINK"&&n.rel==="modulepreload"&&o(n)}).observe(document,{childList:!0,subtree:!0});function l(a){const r={};return a.integrity&&(r.integrity=a.integrity),a.referrerPolicy&&(r.referrerPolicy=a.referrerPolicy),a.crossOrigin==="use-credentials"?r.credentials="include":a.crossOrigin==="anonymous"?r.credentials="omit":r.credentials="same-origin",r}function o(a){if(a.ep)return;a.ep=!0;const r=l(a);fetch(a.href,r)}})();/**
 * @license lucide-react v0.525.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const z=t=>t.replace(/([a-z0-9])([A-Z])/g,"$1-$2").toLowerCase(),C=t=>t.replace(/^([A-Z])|[\s-_]+(\w)/g,(s,l,o)=>o?o.toUpperCase():l.toLowerCase()),p=t=>{const s=C(t);return s.charAt(0).toUpperCase()+s.slice(1)},g=(...t)=>t.filter((s,l,o)=>!!s&&s.trim()!==""&&o.indexOf(s)===l).join(" ").trim(),E=t=>{for(const s in t)if(s.startsWith("aria-")||s==="role"||s==="title")return!0};/**
 * @license lucide-react v0.525.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */var M={xmlns:"http://www.w3.org/2000/svg",width:24,height:24,viewBox:"0 0 24 24",fill:"none",stroke:"currentColor",strokeWidth:2,strokeLinecap:"round",strokeLinejoin:"round"};/**
 * @license lucide-react v0.525.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const O=i.forwardRef(({color:t="currentColor",size:s=24,strokeWidth:l=2,absoluteStrokeWidth:o,className:a="",children:r,iconNode:n,...d},v)=>i.createElement("svg",{ref:v,...M,width:s,height:s,stroke:t,strokeWidth:o?Number(l)*24/Number(s):l,className:g("lucide",a),...!r&&!E(d)&&{"aria-hidden":"true"},...d},[...n.map(([N,b])=>i.createElement(N,b)),...Array.isArray(r)?r:[r]]));/**
 * @license lucide-react v0.525.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const c=(t,s)=>{const l=i.forwardRef(({className:o,...a},r)=>i.createElement(O,{ref:r,iconNode:s,className:g(`lucide-${z(p(t))}`,`lucide-${t}`,o),...a}));return l.displayName=p(t),l};/**
 * @license lucide-react v0.525.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const $=[["path",{d:"m12 19-7-7 7-7",key:"1l729n"}],["path",{d:"M19 12H5",key:"x3x0zl"}]],I=c("arrow-left",$);/**
 * @license lucide-react v0.525.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const S=[["path",{d:"M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z",key:"hh9hay"}],["path",{d:"m3.3 7 8.7 5 8.7-5",key:"g66t2b"}],["path",{d:"M12 22V12",key:"d0xqtd"}]],B=c("box",S);/**
 * @license lucide-react v0.525.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const D=[["ellipse",{cx:"12",cy:"5",rx:"9",ry:"3",key:"msslwz"}],["path",{d:"M3 5V19A9 3 0 0 0 21 19V5",key:"1wlel7"}],["path",{d:"M3 12A9 3 0 0 0 21 12",key:"mv7ke4"}]],F=c("database",D);/**
 * @license lucide-react v0.525.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const U=[["path",{d:"M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",key:"5wwlr5"}],["path",{d:"M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",key:"1d0kgt"}]],V=c("house",U);/**
 * @license lucide-react v0.525.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const H=[["path",{d:"M13 7 8.7 2.7a2.41 2.41 0 0 0-3.4 0L2.7 5.3a2.41 2.41 0 0 0 0 3.4L7 13",key:"orapub"}],["path",{d:"m8 6 2-2",key:"115y1s"}],["path",{d:"m18 16 2-2",key:"ee94s4"}],["path",{d:"m17 11 4.3 4.3c.94.94.94 2.46 0 3.4l-2.6 2.6c-.94.94-2.46.94-3.4 0L11 17",key:"cfq27r"}],["path",{d:"M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z",key:"1a8usu"}],["path",{d:"m15 5 4 4",key:"1mk7zo"}]],Z=c("pencil-ruler",H);/**
 * @license lucide-react v0.525.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const q=[["path",{d:"M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z",key:"1xq2db"}]],m=c("zap",q),y="myvisit-role",w="myvisit-user-name";function K(){return typeof window>"u"?"admin":window.localStorage.getItem(y)||"admin"}function T(t){typeof window>"u"||window.localStorage.setItem(y,t)}function Y(){return typeof window>"u"?"local-user":window.localStorage.getItem(w)||"local-user"}function W(t){typeof window>"u"||window.localStorage.setItem(w,t||"local-user")}function X(){const[t,s]=i.useState(K()),[l,o]=i.useState(Y()),a=n=>{const d=n.target.value;s(d),T(d)},r=()=>{W(l)};return e.jsx("nav",{className:"fixed inset-x-0 top-0 z-50 border-b border-slate-200 bg-white/95 backdrop-blur",children:e.jsxs("div",{className:"mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6",children:[e.jsxs(f,{to:"/",className:"flex items-center gap-3",children:[e.jsx("div",{className:"rounded-xl bg-slate-900 p-2 text-white",children:e.jsx(m,{size:18})}),e.jsxs("div",{className:"flex flex-col",children:[e.jsx("span",{className:"text-sm font-black uppercase tracking-[0.2em] text-slate-900",children:"MyVisit"}),e.jsx("span",{className:"text-xs text-slate-500",children:"Electrical Planning"})]})]}),e.jsxs("div",{className:"flex items-center gap-2",children:[e.jsxs(x,{to:"/",end:!0,className:({isActive:n})=>`flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition ${n?"bg-slate-900 text-white":"text-slate-600 hover:bg-slate-100"}`,children:[e.jsx(V,{size:16}),"בית"]}),e.jsxs(x,{to:"/floorplan-3d",className:({isActive:n})=>`flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition ${n?"bg-emerald-600 text-white":"text-slate-600 hover:bg-slate-100"}`,children:[e.jsx(B,{size:16}),"שרטוט ל-3D"]}),e.jsxs(x,{to:"/electrical",className:({isActive:n})=>`flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition ${n?"bg-blue-600 text-white":"text-slate-600 hover:bg-slate-100"}`,children:[e.jsx(m,{size:16}),"שרטוט חשמל"]}),e.jsxs("div",{className:"ml-2 hidden items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-2 md:flex",children:[e.jsx("input",{type:"text",value:l,onChange:n=>o(n.target.value),onBlur:r,className:"w-24 bg-transparent text-xs text-slate-600 outline-none",placeholder:"User"}),e.jsxs("select",{value:t,onChange:a,className:"bg-transparent text-xs font-medium text-slate-700 outline-none",children:[e.jsx("option",{value:"viewer",children:"viewer"}),e.jsx("option",{value:"editor",children:"editor"}),e.jsx("option",{value:"admin",children:"admin"})]})]})]})]})})}function G(){return e.jsxs("div",{className:"min-h-screen flex flex-col bg-slate-100 text-slate-900",children:[e.jsx(X,{}),e.jsx("main",{className:"flex-grow pt-20",children:e.jsx(_,{})}),e.jsx("footer",{className:"border-t border-slate-200 bg-white py-6 text-center text-sm text-slate-500",children:e.jsxs("p",{children:["© ",new Date().getFullYear()," MyVisit"]})})]})}function J(){return e.jsxs("main",{className:"welcome-page",dir:"rtl",lang:"he",children:[e.jsx("div",{className:"ambient-glow","aria-hidden":"true"}),e.jsxs("section",{className:"hero-shell",children:[e.jsxs("div",{className:"hero-copy",children:[e.jsx("p",{className:"eyebrow",children:"מודול משולב מתוך Fina"}),e.jsx("h1",{children:"תכנון חשמל מקצועי בתוך האתר המקורי"}),e.jsx("p",{className:"hero-text",children:"העורך כולל סימבולים, שכבות, חישובי עומסים, שמירת פרויקטים, יצוא PDF ו-DXF וחיבור לשרת המקורי."}),e.jsxs(f,{to:"/electrical",className:"hero-link",children:["לעורך החשמל",e.jsx(I,{size:18})]})]}),e.jsxs("div",{className:"hero-grid",children:[e.jsxs("div",{className:"feature-tile",children:[e.jsx(m,{size:22}),e.jsx("span",{children:"שרטוט לפי שכבות"})]}),e.jsxs("div",{className:"feature-tile",children:[e.jsx(Z,{size:22}),e.jsx("span",{children:"סימבולים ועריכה"})]}),e.jsxs("div",{className:"feature-tile",children:[e.jsx(F,{size:22}),e.jsx("span",{children:"שמירה בשרת"})]})]})]})]})}const h=i.lazy(()=>j(()=>import("./ElectricalCADPage-BFDNCOFE.js").then(t=>t.f),__vite__mapDeps([0,1,2,3,4,5]))),Q=i.lazy(()=>j(()=>import("./FloorPlanStudioPage-BJI6KElA.js"),__vite__mapDeps([6,1,2,3,4,5])));function ee(){return e.jsx("div",{className:"flex min-h-[50vh] items-center justify-center text-slate-500",children:"טוען עמוד..."})}function te(){return e.jsx(i.Suspense,{fallback:e.jsx(ee,{}),children:e.jsx(k,{children:e.jsxs(u,{path:"/",element:e.jsx(G,{}),children:[e.jsx(u,{index:!0,element:e.jsx(J,{})}),e.jsx(u,{path:"floorplan-3d",element:e.jsx(Q,{})}),e.jsx(u,{path:"electrical",element:e.jsx(h,{})}),e.jsx(u,{path:"electrical/:projectId",element:e.jsx(h,{})})]})})})}L.createRoot(document.getElementById("root")).render(e.jsx(A.StrictMode,{children:e.jsxs(P,{future:{v7_startTransition:!0,v7_relativeSplatPath:!0},children:[e.jsx(te,{}),e.jsx(R,{position:"top-center"})]})}));export{B,V as H,m as Z,Y as a,c,K as g};
