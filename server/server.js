import express from 'express';
import bcrypt from 'bcryptjs';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const app = express();
const PORT = process.env.PORT || 3000;
const DATA_DIR = path.join(__dirname, 'data');
const USERS_FILE = path.join(DATA_DIR, 'users.json');
const tokens = new Map();

fs.mkdirSync(DATA_DIR, { recursive: true });
if (!fs.existsSync(USERS_FILE)) fs.writeFileSync(USERS_FILE, '[]', 'utf8');

app.use(express.json({ limit: '12mb' }));
app.use(express.static(path.join(__dirname, 'public')));

function loadUsers(){ try { return JSON.parse(fs.readFileSync(USERS_FILE,'utf8')); } catch { return []; } }
function saveUsers(users){ fs.writeFileSync(USERS_FILE, JSON.stringify(users,null,2)); }
function today(){ return new Date().toISOString().slice(0,10); }
function safeUser(u){ const {passwordHash,...rest}=u; return rest; }
function makeToken(session){ const t=crypto.randomBytes(32).toString('hex'); tokens.set(t,{...session,created:Date.now()}); return t; }
function auth(req,res,next){ const t=(req.headers.authorization||'').replace(/^Bearer\s+/,''); const s=tokens.get(t); if(!s) return res.status(401).json({error:'No autorizado'}); req.session=s; next(); }
function adminOnly(req,res,next){ if(req.session?.role!=='admin') return res.status(403).json({error:'Solo administrador'}); next(); }
function userRecord(username){ return loadUsers().find(u=>u.username.toLowerCase()===String(username).toLowerCase()); }

app.get('/api/health',(req,res)=>res.json({ok:true,name:'Dasha'}));

app.post('/api/login', async (req,res)=>{
  const {username,password}=req.body||{};
  if(!username||!password) return res.status(400).json({error:'Usuario y contraseña requeridos'});
  const adminUser=process.env.ADMIN_USER;
  const adminPass=process.env.ADMIN_PASSWORD;
  if(adminUser && adminPass && username===adminUser && password===adminPass){
    return res.json({token:makeToken({username,role:'admin'}),role:'admin',username});
  }
  const users=loadUsers();
  const u=users.find(x=>x.username.toLowerCase()===String(username).toLowerCase());
  if(!u || !(await bcrypt.compare(password,u.passwordHash))) return res.status(401).json({error:'Credenciales incorrectas'});
  if(!u.active) return res.status(403).json({error:'Usuario bloqueado'});
  if(u.expiresAt && Date.now()>new Date(u.expiresAt).getTime()) return res.status(403).json({error:'Acceso vencido'});
  return res.json({token:makeToken({username:u.username,role:'user'}),role:'user',username:u.username});
});

app.post('/api/logout',auth,(req,res)=>{
  const t=(req.headers.authorization||'').replace(/^Bearer\s+/,''); tokens.delete(t); res.json({ok:true});
});

app.get('/api/admin/users',auth,adminOnly,(req,res)=>res.json(loadUsers().map(safeUser)));

app.post('/api/admin/users',auth,adminOnly,async(req,res)=>{
  const {username,password,dailyLimit=30,expiresAt=null,active=true}=req.body||{};
  if(!username||!password) return res.status(400).json({error:'Usuario y contraseña requeridos'});
  const users=loadUsers();
  if(users.some(u=>u.username.toLowerCase()===String(username).toLowerCase())) return res.status(409).json({error:'Ese usuario ya existe'});
  const u={id:crypto.randomUUID(),username:String(username).trim(),passwordHash:await bcrypt.hash(password,10),active:Boolean(active),dailyLimit:Number(dailyLimit)||30,expiresAt:expiresAt||null,usedDate:today(),usedToday:0,createdAt:new Date().toISOString()};
  users.push(u); saveUsers(users); res.json(safeUser(u));
});

app.patch('/api/admin/users/:id',auth,adminOnly,async(req,res)=>{
  const users=loadUsers(); const i=users.findIndex(u=>u.id===req.params.id); if(i<0) return res.status(404).json({error:'Usuario no encontrado'});
  const allowed=['active','dailyLimit','expiresAt','username']; for(const k of allowed) if(k in req.body) users[i][k]=req.body[k];
  if(req.body.password) users[i].passwordHash=await bcrypt.hash(req.body.password,10);
  saveUsers(users); res.json(safeUser(users[i]));
});

app.delete('/api/admin/users/:id',auth,adminOnly,(req,res)=>{
  const users=loadUsers(); const next=users.filter(u=>u.id!==req.params.id); if(next.length===users.length) return res.status(404).json({error:'Usuario no encontrado'}); saveUsers(next); res.json({ok:true});
});

function checkQuota(username){
  const users=loadUsers(); const i=users.findIndex(u=>u.username===username); if(i<0) return {ok:false,error:'Usuario no encontrado'};
  const u=users[i]; if(u.usedDate!==today()){u.usedDate=today();u.usedToday=0;}
  if(u.dailyLimit>0 && u.usedToday>=u.dailyLimit) return {ok:false,error:'Límite diario alcanzado'};
  u.usedToday+=1; users[i]=u; saveUsers(users); return {ok:true};
}

function extractText(data){
  if(typeof data?.output_text==='string' && data.output_text) return data.output_text;
  const out=[]; for(const item of data?.output||[]) for(const c of item?.content||[]) if(c?.type==='output_text' && c?.text) out.push(c.text);
  return out.join('\n').trim();
}

app.post('/api/chat',auth,async(req,res)=>{
  try{
    if(req.session.role==='user'){ const q=checkQuota(req.session.username); if(!q.ok) return res.status(429).json({error:q.error}); }
    if(!process.env.OPENAI_API_KEY) return res.status(503).json({error:'Falta configurar OPENAI_API_KEY en el servidor'});
    const message=String(req.body?.message||'').trim(); if(!message) return res.status(400).json({error:'Mensaje vacío'});
    const r=await fetch('https://api.openai.com/v1/responses',{method:'POST',headers:{'Authorization':`Bearer ${process.env.OPENAI_API_KEY}`,'Content-Type':'application/json'},body:JSON.stringify({model:process.env.OPENAI_MODEL||'gpt-5.6-luna',input:message})});
    const data=await r.json(); if(!r.ok) return res.status(r.status).json({error:data?.error?.message||'Error de OpenAI'});
    res.json({text:extractText(data)||'No recibí texto de respuesta.'});
  }catch(e){res.status(500).json({error:e.message});}
});

app.post('/api/image',auth,async(req,res)=>{
  try{
    if(req.session.role==='user'){ const q=checkQuota(req.session.username); if(!q.ok) return res.status(429).json({error:q.error}); }
    if(!process.env.OPENAI_API_KEY) return res.status(503).json({error:'Falta configurar OPENAI_API_KEY en el servidor'});
    const prompt=String(req.body?.prompt||'').trim(); if(!prompt) return res.status(400).json({error:'Descripción vacía'});
    const r=await fetch('https://api.openai.com/v1/images/generations',{method:'POST',headers:{'Authorization':`Bearer ${process.env.OPENAI_API_KEY}`,'Content-Type':'application/json'},body:JSON.stringify({model:'gpt-image-2',prompt,size:'1024x1024'})});
    const data=await r.json(); if(!r.ok) return res.status(r.status).json({error:data?.error?.message||'Error al generar imagen'});
    const item=data?.data?.[0]||{}; res.json({b64:item.b64_json||null,url:item.url||null});
  }catch(e){res.status(500).json({error:e.message});}
});

app.get('*',(req,res)=>res.sendFile(path.join(__dirname,'public','index.html')));
app.listen(PORT,'0.0.0.0',()=>console.log(`Dasha server running on ${PORT}`));
