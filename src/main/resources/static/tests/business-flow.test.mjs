import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createMockService, STORAGE_KEY} from '../mock-service.mjs';
import {exampleDraft, clarificationQuestions, currentUser} from '../mock-data.mjs';
import {escapeHtml, ratingLevel} from '../ui-utils.mjs';

function memoryStorage(){const data=new Map();return {getItem:key=>data.get(key)||null,setItem:(key,value)=>data.set(key,value)};}
const answers=Object.fromEntries(clarificationQuestions.map(question=>[question.id,question.example]));

test('draft → questions → editable card → analysis → publication → refresh',async()=>{
  const storage=memoryStorage(),service=createMockService({storage,delay:0});
  let project=await service.createDraft(exampleDraft);
  assert.equal(project.status,'DRAFT');
  assert.equal((await service.listCatalog()).some(item=>item.id===project.id),false);
  const questions=await service.getQuestions(project.id);
  assert.equal(questions.length,3);
  await assert.rejects(service.saveAnswers(project.id,{}));
  project=await service.saveAnswers(project.id,answers);
  assert.equal(project.status,'CARD');
  assert.equal(project.audience,answers.audience);
  project=await service.updateCard(project.id,{...project,title:'Фермерская корзина',offer:project.offer+' Пилот длится 8 недель.'});
  assert.equal(project.title,'Фермерская корзина');
  project=await service.analyze(project.id);
  assert.equal(project.status,'RATED');
  assert.equal(project.analysis.score,project.analysis.breakdown.reduce((sum,part)=>sum+part.score,0));
  assert.ok(project.analysis.recommendations.length);
  await service.publish(project.id);
  assert.ok((await service.listCatalog()).some(item=>item.id===project.id));
  const reloaded=createMockService({storage,delay:0});
  assert.equal((await reloaded.getBusiness(project.id)).status,'PUBLISHED');
  assert.equal((await reloaded.getBusiness(project.id)).title,'Фермерская корзина');
});

test('catalog filters and sorting operate on published data',async()=>{
  const service=createMockService({delay:0});
  const all=await service.listCatalog();
  assert.ok(all.length>=6);
  assert.ok(all.every((item,index)=>index===0||all[index-1].analysis.score>=item.analysis.score));
  const filtered=await service.listCatalog({city:'Алматы',category:'Производство',minimumScore:80,search:'Greenpack'});
  assert.equal(filtered.length,1);
  assert.equal(filtered[0].title,'Greenpack');
  assert.equal((await service.listCatalog({search:'не существующий проект'})).length,0);
  const ascending=await service.listCatalog({sort:'rating-asc'});
  assert.ok(ascending[0].analysis.score<=ascending.at(-1).analysis.score);
});

test('apply creates an outgoing response, rejects duplicates and persists',async()=>{
  const storage=memoryStorage(),service=createMockService({storage,delay:0});
  const input={message:'Предлагаем площадку для проверки вашего решения.',contact:'demo@example.com'};
  const application=await service.apply('seed-1',input);
  assert.equal(application.status,'PENDING');
  assert.equal((await service.listApplications({direction:'sent'})).length,1);
  assert.equal((await service.listApplications({direction:'received'})).some(item=>item.id===application.id),false);
  await assert.rejects(service.apply('seed-1',input),/уже откликнулись/);
  await assert.rejects(service.apply('my-seed',input),/автор/);
  assert.equal((await createMockService({storage,delay:0}).listApplications({direction:'sent'}))[0].message,input.message);
});

test('accept and reject distinct received applications, persist decisions',async()=>{
  const storage=memoryStorage(),service=createMockService({storage,delay:0});
  await service.respond('response-1','ACCEPTED');
  await service.respond('response-2','REJECTED');
  const reloaded=createMockService({storage,delay:0});
  const all=await reloaded.listApplications({direction:'received'});
  assert.equal(all.find(item=>item.id==='response-1').status,'ACCEPTED');
  assert.equal(all.find(item=>item.id==='response-2').status,'REJECTED');
  assert.equal((await reloaded.listApplications({status:'PENDING'})).length,0);
  await assert.rejects(service.respond('response-1','REJECTED'),/уже обработан/);
});

test('editing invalidates analysis and premature publication is blocked',async()=>{
  const service=createMockService({delay:0});
  let project=await service.createDraft(exampleDraft);
  await assert.rejects(service.publish(project.id),/оценку/);
  await service.getQuestions(project.id);project=await service.saveAnswers(project.id,answers);
  project=await service.analyze(project.id);project=await service.updateCard(project.id,project);
  assert.equal(project.analysis,null);assert.equal(project.status,'CARD');
  await assert.rejects(service.publish(project.id),/оценку/);
  await assert.rejects(service.updateCard('seed-1',project),/автор/);
});

test('storage failures do not report successful mutations or discard existing state',async()=>{
  const storage={getItem:()=>null,setItem:()=>{throw new Error('Quota exceeded');}};
  const service=createMockService({storage,delay:0});
  await assert.rejects(service.createDraft(exampleDraft),/сохранить/);
  assert.equal((await service.listMine()).length,1);
  const malformed={getItem:()=>'{invalid json',setItem:()=>{}};
  assert.ok((await createMockService({storage:malformed,delay:0}).listCatalog()).length);
});

test('UI escapes user content and uses consistent rating boundaries',()=>{
  assert.equal(escapeHtml('<script>"&'), '&lt;script&gt;&quot;&amp;');
  assert.equal(ratingLevel(80).tone,'high');assert.equal(ratingLevel(79).tone,'medium');assert.equal(ratingLevel(59).tone,'low');
});
