import {api} from './api.mjs';
import {escapeHtml as e, statusLabels, stageFor, ratingLevel, formatDate} from './ui-utils.mjs';

const icons = {
  arrow:'<path d="M5 12h14m-5-5 5 5-5 5"/>', back:'<path d="M19 12H5m5-5-5 5 5 5"/>',
  plus:'<path d="M12 5v14M5 12h14"/>', check:'<path d="m5 12 4 4L19 6"/>', close:'<path d="m6 6 12 12M6 18 18-12"/>',
  spark:'<path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3Z"/>',
  pin:'<path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 0 1 14 0Z"/><circle cx="12" cy="10" r="2"/>',
  briefcase:'<rect x="3" y="7" width="18" height="14" rx="2"/><path d="M8 7V3h8v4M3 12c6 4 12 4 18 0M12 12v4"/>',
  chart:'<path d="M4 3v17h17M8 15l4-5 4 2 5-7"/>',
  mail:'<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 6 9 7 9-7"/>',
  clock:'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  edit:'<path d="m16 3 5 5L9 20l-6 1 1-6L16 3Z"/>',
  search:'<circle cx="10" cy="10" r="6"/><path d="m15 15 6 6"/>',
  shield:'<path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z"/><path d="m8 11 3 3 5-5"/>',
};
const icon = name => `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${icons[name]||icons.briefcase}</svg>`;
const main = document.querySelector('#main');
const dialog = document.querySelector('#application-dialog');
let meta, token = 0, toastTimer, catalogFilters = {}, applicationFilters = {direction:'received',status:''};
const formMemory = new Map();
const workflow = [['draft','Черновик'],['questions','Вопросы ИИ'],['card','Карточка'],['rating','Рейтинг'],['published','Публикация']];

function notify(message) {
  const toast=document.querySelector('#toast');toast.textContent=message;toast.hidden=false;
  clearTimeout(toastTimer);toastTimer=setTimeout(()=>toast.hidden=true,4500);
}
function report(error, target='#action-error') {
  const box=document.querySelector(target);
  if(box){box.innerHTML=`<div class="error" role="alert">${e(error.message||'Не удалось выполнить действие. Попробуйте ещё раз.')}</div>`;box.scrollIntoView({block:'nearest'});}
  else notify(error.message);
}
function loading(label='Загружаем пространство…'){return `<div class="loading" role="status"><span class="spinner"></span>${e(label)}</div>`;}
function statusBadge(status){return `<span class="status-badge ${e(status.toLowerCase())}"><span></span>${e(statusLabels[status]||status)}</span>`;}
function ratingBadge(analysis){const level=ratingLevel(analysis.score);return `<span class="rating-badge ${level.tone}">${icon('chart')}<strong>${analysis.score}</strong><span>· ${level.label}</span></span>`;}
function options(items,value,placeholder){return `<option value="">${e(placeholder)}</option>${items.map(item=>`<option value="${e(item)}" ${item===value?'selected':''}>${e(item)}</option>`).join('')}`;}
function field(name,label,value,{textarea=false,min=1,max=2000,placeholder='',hint='',rows=3}={}) {
  return `<div class="field"><label for="${name}">${label}</label>${textarea?`<textarea id="${name}" name="${name}" rows="${rows}" minlength="${min}" maxlength="${max}" required ${hint?`aria-describedby="${name}-hint"`:''} placeholder="${e(placeholder)}">${e(value)}</textarea>`:`<input id="${name}" name="${name}" value="${e(value)}" minlength="${min}" maxlength="${max}" required placeholder="${e(placeholder)}">`}${hint?`<small id="${name}-hint">${e(hint)}</small>`:''}</div>`;
}
function selectors(item){return `<div class="fields-three"><div class="field"><label for="category">Сфера</label><select id="category" name="category" required>${options(meta.categories,item.category,'Выберите сферу')}</select></div><div class="field"><label for="city">Город</label><select id="city" name="city" required>${options(meta.cities,item.city,'Выберите город')}</select></div><div class="field"><label for="partnershipType">Ищете</label><select id="partnershipType" name="partnershipType" required>${options(meta.partnershipTypes,item.partnershipType,'Формат сотрудничества')}</select></div></div>`;}
function formData(form){return Object.fromEntries(new FormData(form));}
function remember(form,key){form.addEventListener('input',()=>formMemory.set(key,formData(form)));}
async function pending(container,label,work){
  if(container.dataset.busy==='true')return;
  container.dataset.busy='true';container.setAttribute('aria-busy','true');
  const controls=[...container.querySelectorAll('button,input,textarea,select')];
  const wasDisabled=controls.map(control=>control.disabled);controls.forEach(control=>control.disabled=true);
  const button=container.matches('button')?container:container.querySelector('button[type=submit]');
  const original=button?.innerHTML;if(button){button.disabled=true;button.innerHTML=`<span class="spinner small"></span>${e(label)}`;}
  try {return await work();} finally {
    container.dataset.busy='false';container.removeAttribute('aria-busy');controls.forEach((control,index)=>control.disabled=wasDisabled[index]);
    if(button){button.innerHTML=original;button.disabled=false;}
  }
}
function go(path){if(location.hash===`#${path}`)route();else location.hash=path;}
function pageHeading(eyebrow,title,description,action=''){return `<div class="page-heading"><div><p class="eyebrow">${eyebrow}</p><h1>${title}</h1><p class="lead">${description}</p></div>${action}</div>`;}
function empty(title,text,action=''){return `<div class="empty">${icon('search')}<h2>${title}</h2><p>${text}</p>${action}</div>`;}
function renderWorkflowHeader(stage,item){
  const active=workflow.findIndex(([key])=>key===stage);
  return `<a class="back-link" href="#business">${icon('back')}Мой бизнес</a><div class="workflow-heading"><div><p class="eyebrow">Новая возможность</p><h1>${item?.title?e(item.title):'Расскажите о своём бизнесе'}</h1></div>${statusBadge(item?.status||'DRAFT')}</div><ol class="stepper" aria-label="Этапы создания">${workflow.map(([key,label],index)=>`<li class="${index===active?'active':index<active?'complete':''}" ${index===active?'aria-current="step"':''}><span>${index<active?icon('check'):index+1}</span>${label}</li>`).join('')}</ol>`;
}
function cardMarkup(item,{mine=false}={}){
  return `<article class="business-card"><div class="card-top"><span class="company-logo tone-${meta.categories.indexOf(item.category)%5}">${e(item.title.slice(0,2).toUpperCase())}</span>${item.analysis?ratingBadge(item.analysis):statusBadge(item.status)}</div><div class="card-category">${e(item.category)} <span>·</span> ${e(item.partnershipType)}</div><h2><a href="#${mine?`business/${item.id}/${stageFor(item)}`:`opportunity/${item.id}`}">${e(item.title)}</a></h2><p class="card-summary">${e(item.tagline||item.description)}</p><div class="card-footer"><span>${icon('pin')}${e(item.city)}</span><a href="#${mine?`business/${item.id}/${stageFor(item)}`:`opportunity/${item.id}`}" class="card-link">${mine&&item.status!=='PUBLISHED'?'Продолжить':'Подробнее'}${icon('arrow')}</a></div></article>`;
}
async function businessPage(routeId){
  const items=await api.listMine();if(routeId!==token)return;
  main.innerHTML=`<div class="container">${pageHeading('От идеи к партнёрству','Мой бизнес','Соберите понятное предложение и найдите тех, с кем можно расти.',`<a class="button" href="#business/new">${icon('plus')}Создать проект</a>`)}<section class="intro-strip"><div class="intro-icon">${icon('spark')}</div><div><h2>Хорошее партнёрство начинается с ясности</h2><p>Опишите идею → ответьте на вопросы → проверьте карточку → получите оценку → опубликуйте.</p></div><a href="#business/new" class="text-link">Начать ${icon('arrow')}</a></section><div class="section-title"><h2>Ваши проекты <span class="count">${items.length}</span></h2><span class="muted">Черновики видны только вам</span></div>${items.length?`<div class="card-grid">${items.map(item=>cardMarkup(item,{mine:true})).join('')}</div>`:empty('Первый шаг — ваша идея','Добавьте проект, чтобы подготовить предложение для партнёров.','<a href="#business/new" class="button">Создать проект</a>')}</div>`;
}
async function draftPage(id,routeId){
  const item=id?await api.getBusiness(id):null;if(routeId!==token)return;
  if(item&&item.status!=='DRAFT'){go(`business/${id}/${stageFor(item)}`);return;}
  const key=id||'new',draft=formMemory.get(key)||item||{};
  main.innerHTML=`<div class="container workflow">${renderWorkflowHeader('draft',item)}<div class="two-column"><section class="panel"><div class="panel-heading"><span class="mini-icon">${icon('briefcase')}</span><div><h2>Начнём с самого важного</h2><p>Опишите проект своими словами. Детали уточним на следующем шаге.</p></div></div><form id="draft-form">${field('title','Название проекта',draft.title,{min:3,max:100,placeholder:'Как называется ваш проект?'})}${selectors(draft)}${field('description','Что вы делаете и кого ищете?',draft.description,{textarea:true,min:30,max:3000,rows:6,placeholder:'Для кого ваш продукт? Какую задачу решает? Какая помощь или партнёрство нужны?',hint:'От 30 до 3 000 символов. На следующем шаге можно добавить детали.'})}<div id="action-error"></div><div class="form-actions"><button type="button" class="button ghost" id="example">Заполнить пример</button><button type="submit" class="button">Продолжить ${icon('arrow')}</button></div><p class="form-footnote">Сохраним черновик и подготовим вопросы. В каталоге он пока не появится.</p></form></section><aside><div class="side-panel"><span class="eyebrow">Что будет дальше</span><h2>Идея становится<br>предложением</h2><ul class="benefits"><li>${icon('spark')}Вопросы помогут раскрыть недостающие детали</li><li>${icon('edit')}Карточку можно отредактировать перед публикацией</li><li>${icon('chart')}Рейтинг покажет, что стоит уточнить</li></ul><div class="side-note">Вы решаете, когда проект готов к публикации.</div></div></aside></div></div>`;
  const form=document.querySelector('#draft-form');remember(form,key);
  document.querySelector('#example').onclick=()=>{for(const [name,value] of Object.entries(meta.exampleDraft||{}))if(form.elements[name])form.elements[name].value=value;formMemory.set(key,formData(form));notify('Пример добавлен. Его можно изменить.');};
  form.onsubmit=event=>{event.preventDefault();if(!form.reportValidity())return;const data=formData(form);pending(form,'Сохраняем…',async()=>{try{const saved=id?await api.updateDraft(id,data):await api.createDraft(data);formMemory.delete(key);if(routeId===token)go(`business/${saved.id}/questions`);}catch(error){if(routeId===token)report(error);}});};
}
async function questionsPage(id,routeId){
  const [item,questions]=await Promise.all([api.getBusiness(id),api.getQuestions(id)]);if(routeId!==token)return;
  if(item.status==='PUBLISHED'){go(`business/${id}/published`);return;}
  const remembered=formMemory.get(`answers-${id}`)||item.answers||{};
  main.innerHTML=`<div class="container workflow">${renderWorkflowHeader('questions',{...item,status:'QUESTIONS'})}<div class="two-column"><section class="panel"><div class="panel-heading"><span class="mini-icon">${icon('spark')}</span><div><h2>Давайте уточним предложение</h2><p>${questions.length} вопроса, чтобы будущему партнёру было проще вас понять.</p></div></div><p class="inline-note">Демонстрационные вопросы. Ответы войдут в вашу карточку без изменения смысла.</p><form id="questions-form">${questions.map((q,index)=>`<div class="question-number">Вопрос ${index+1} из ${questions.length}</div>${field(q.id,e(q.title),remembered[q.id],{textarea:true,min:10,max:2000,rows:3,hint:q.hint,placeholder:'Ваш ответ…'})}`).join('')}<div id="action-error"></div><div class="form-actions"><button type="button" class="button ghost" id="answer-examples">Подставить примеры ответов</button><button type="submit" class="button">Получить карточку ${icon('arrow')}</button></div></form></section><aside class="side-panel"><span class="eyebrow">Ваш проект</span><h2>${e(item.title)}</h2><p>${e(item.description)}</p><div class="side-note">Конкретные ответы полезнее общих обещаний. Если данных пока нет, опишите, как планируете их получить.</div></aside></div></div>`;
  const form=document.querySelector('#questions-form');remember(form,`answers-${id}`);
  document.querySelector('#answer-examples').onclick=()=>{questions.forEach(q=>form.elements[q.id].value=q.example||'');formMemory.set(`answers-${id}`,formData(form));notify('Примеры добавлены. Уточните их под свой проект.');};
  form.onsubmit=event=>{event.preventDefault();if(!form.reportValidity())return;const answers=formData(form);pending(form,'Готовим карточку…',async()=>{try{await api.saveAnswers(id,answers);formMemory.delete(`answers-${id}`);if(routeId===token)go(`business/${id}/card`);}catch(error){if(routeId===token)report(error);}});};
}
async function editableCardPage(id,routeId){
  const item=await api.getBusiness(id);if(routeId!==token)return;
  if(!['CARD','RATED'].includes(item.status)){go(`business/${id}/${stageFor(item)}`);return;}
  const draft={...item,...formMemory.get(`card-${id}`)};
  main.innerHTML=`<div class="container workflow">${renderWorkflowHeader('card',item)}<div class="two-column card-editor"><section class="panel"><div class="panel-heading"><span class="mini-icon">${icon('edit')}</span><div><h2>Карточка готова к проверке</h2><p>Уточните формулировки. После сохранения можно получить рейтинг.</p></div></div><form id="card-form">${field('title','Название проекта',draft.title,{min:3,max:100})}${field('tagline','Краткое предложение',draft.tagline,{min:10,max:160,hint:'Одно предложение для карточки в каталоге.'})}${selectors(draft)}${field('description','О проекте',draft.description,{textarea:true,min:30,max:3000})}${field('audience','Клиенты и их задача',draft.audience,{textarea:true,min:10,max:2000})}${field('traction','Текущие результаты',draft.traction,{textarea:true,min:10,max:2000})}${field('offer','Предложение партнёру',draft.offer,{textarea:true,min:10,max:2000})}<div id="action-error"></div><div class="form-actions"><button type="submit" value="save" class="button secondary">Сохранить карточку</button><button type="submit" value="analyze" class="button">Посмотреть рейтинг ${icon('arrow')}</button></div><p class="form-footnote">Любое изменение карточки обновляет черновик и сбрасывает предыдущую оценку.</p></form></section><aside><div class="preview-label">Так вас увидят в каталоге</div><div id="card-preview">${cardMarkup({...draft,analysis:null})}</div><div class="side-note standalone">${icon('shield')}Публикация произойдёт только после вашего подтверждения.</div></aside></div></div>`;
  const form=document.querySelector('#card-form');remember(form,`card-${id}`);
  form.addEventListener('input',()=>document.querySelector('#card-preview').innerHTML=cardMarkup({...item,...formData(form),analysis:null}));
  form.onsubmit=event=>{event.preventDefault();if(!form.reportValidity())return;const data=formData(form),analyze=event.submitter?.value==='analyze';pending(form,analyze?'Готовим оценку…':'Сохраняем…',async()=>{try{await api.updateCard(id,data);formMemory.delete(`card-${id}`);if(analyze){await api.analyze(id);if(routeId===token)go(`business/${id}/rating`);}else notify('Карточка сохранена. Можно переходить к рейтингу.');}catch(error){if(routeId===token)report(error);}});};
}
function analysisMarkup(analysis,{compact=false}={}){
  const level=ratingLevel(analysis.score);
  return `<div class="analysis ${compact?'compact':''}"><div class="rating-summary ${level.tone}"><div class="rating-orb" style="--score:${analysis.score}%"><span><strong>${analysis.score}</strong><small>из 100</small></span></div><div><span class="eyebrow">Рейтинг карточки</span><h2>${level.label}</h2><p>${level.description}</p></div><span class="badge">Демо-оценка</span></div><p class="analysis-note">${e(analysis.explanation)}</p><div class="panel"><h2>Из чего складывается оценка</h2><div class="breakdown">${analysis.breakdown.map(part=>`<div class="breakdown-item"><div><strong>${e(part.title)}</strong><span>${part.score} <small>/ ${part.weight}</small></span></div><progress value="${part.score}" max="${part.weight}" aria-label="${e(part.title)}"></progress><p>${e(part.detail)}</p></div>`).join('')}</div></div><div class="analysis-columns"><section class="panel"><h3>${icon('check')}Сильные стороны</h3><ul class="analysis-list">${analysis.strengths.map(text=>`<li>${e(text)}</li>`).join('')}</ul></section><section class="panel"><h3>${icon('shield')}Что стоит учесть</h3><ul class="analysis-list">${analysis.risks.map(text=>`<li>${e(text)}</li>`).join('')}</ul></section></div><section class="panel recommendations"><div class="section-title"><h2>${icon('spark')}Рекомендации</h2><span class="muted">Следующий шаг к сильной карточке</span></div>${analysis.recommendations.map((item,index)=>`<div class="recommendation"><span>${index+1}</span><div><h3>${e(item.title)}</h3><p>${e(item.text)}</p></div></div>`).join('')}</section></div>`;
}
async function ratingPage(id,routeId){
  const item=await api.getBusiness(id);if(routeId!==token)return;
  if(item.status!=='RATED'){go(`business/${id}/${stageFor(item)}`);return;}
  main.innerHTML=`<div class="container workflow">${renderWorkflowHeader('rating',item)}${analysisMarkup(item.analysis)}<div class="publish-bar"><div><h2>Готовы обсудить партнёрство?</h2><p>После публикации карточка появится в каталоге.</p></div><div class="button-row"><a class="button secondary" href="#business/${id}/card">${icon('edit')}Улучшить карточку</a><button class="button" id="publish">Опубликовать ${icon('arrow')}</button></div></div><div id="action-error"></div></div>`;
  document.querySelector('#publish').onclick=event=>pending(event.currentTarget,'Публикуем…',async()=>{try{await api.publish(id);if(routeId===token){notify('Проект опубликован в каталоге');go(`business/${id}/published`);}}catch(error){if(routeId===token)report(error);}});
}
async function publishedPage(id,routeId){
  const item=await api.getBusiness(id);if(routeId!==token)return;
  if(item.status!=='PUBLISHED'){go(`business/${id}/${stageFor(item)}`);return;}
  main.innerHTML=`<div class="container workflow">${renderWorkflowHeader('published',item)}<section class="published-panel"><div class="success-icon">${icon('check')}</div><p class="eyebrow">Следующий шаг сделан</p><h1>Ваш проект в каталоге</h1><p>Карточка опубликована. Теперь потенциальные партнёры могут познакомиться с предложением и откликнуться.</p><div class="published-card">${cardMarkup(item)}</div><div class="button-row"><a class="button" href="#opportunity/${id}">Открыть карточку ${icon('arrow')}</a><a class="button secondary" href="#catalog">Перейти в каталог</a></div></section></div>`;
}
async function catalogPage(routeId){
  main.innerHTML=`<div class="container">${pageHeading('Здесь начинаются партнёрства','Каталог возможностей','Найдите проект, которому пригодится ваш опыт, продукт или команда.',`<a class="button" href="#business/new">${icon('plus')}Добавить проект</a>`)}<form class="filters" id="catalog-filters"><div class="search-field">${icon('search')}<label class="sr-only" for="search">Поиск по проектам</label><input id="search" name="search" placeholder="Название или ключевые слова" value="${e(catalogFilters.search||'')}"></div><div><label for="filter-category">Сфера</label><select id="filter-category" name="category">${options(meta.categories,catalogFilters.category,'Все сферы')}</select></div><div><label for="filter-city">Город</label><select id="filter-city" name="city">${options(meta.cities,catalogFilters.city,'Все города')}</select></div><div><label for="filter-type">Формат</label><select id="filter-type" name="partnershipType">${options(meta.partnershipTypes,catalogFilters.partnershipType,'Все форматы')}</select></div><div><label for="filter-rating">Рейтинг</label><select id="filter-rating" name="minimumScore"><option value="0">Любой</option value="80" ${catalogFilters.minimumScore==='80'?'selected':''}>Высокий · от 80</option><option value="60" ${catalogFilters.minimumScore==='60'?'selected':''}>От 60</option></select></div><button class="button" type="submit">Найти</button></form><div class="catalog-toolbar"><p id="catalog-count" role="status">Загружаем проекты…</p><div><button class="text-link" id="reset-filters">Сбросить фильтры</button><label class="sr-only" for="sort">Сортировка</label><select id="sort"><option value="rating">Сначала высокий рейтинг</option><option value="rating-asc">Сначала низкий рейтинг</option><option value="newest">Сначала новые</option></select></div></div><div id="catalog-results">${loading()}</div></div>`;
  const form=document.querySelector('#catalog-filters'),sort=document.querySelector('#sort');sort.value=catalogFilters.sort||'rating';let request=0;
  const refresh=async()=>{const sequence=++request;catalogFilters={...formData(form),sort:sort.value};const results=document.querySelector('#catalog-results');results.innerHTML=loading('Ищем подходящие проекты…');try{const items=await api.listCatalog(catalogFilters);if(routeId!==token||sequence!==request)return;document.querySelector('#catalog-count').textContent=`Найдено проектов: ${items.length}`;results.innerHTML=items.length?`<div class="card-grid">${items.map(item=>cardMarkup(item)).join('')}</div>`:empty('Пока нет совпадений','Попробуйте другую сферу, город или снизьте минимальный рейтинг.');}catch(error){if(routeId===token&&sequence===request){results.innerHTML='<div id="catalog-error"></div>';report(error,'#catalog-error');}}};
  form.onsubmit=event=>{event.preventDefault();refresh();};form.querySelectorAll('select').forEach(select=>select.onchange=refresh);sort.onchange=refresh;
  document.querySelector('#reset-filters').onclick=()=>{for(const element of form.elements)if(element.name)element.value=element.name==='minimumScore'?'0':'';sort.value='rating';refresh();};
  await refresh();
}
async function detailPage(id,routeId){
  const [item,sent]=await Promise.all([api.getBusiness(id),api.listApplications({direction:'sent'})]);if(routeId!==token)return;
  if(item.status!=='PUBLISHED'){go(item.ownerId===meta.user.id?`business/${id}/${stageFor(item)}`:'catalog');return;}
  const own=item.ownerId===meta.user.id,application=sent.find(entry=>entry.opportunityId===id);
  main.innerHTML=`<div class="container detail"><a class="back-link" href="#catalog">${icon('back')}В каталог</a><div class="detail-heading"><span class="company-logo large tone-${meta.categories.indexOf(item.category)%5}">${e(item.title.slice(0,2).toUpperCase())}</span><div><p class="eyebrow">${e(item.category)} · ${e(item.city)}</p><h1>${e(item.title)}</h1><p class="lead">${e(item.tagline)}</p></div>${ratingBadge(item.analysis)}</div><div class="two-column"><div><section class="panel detail-copy"><h2>О проекте</h2><p>${e(item.description)}</p><h3>Клиенты и их задача</h3><p>${e(item.audience)}</p><h3>Текущие результаты</h3><p>${e(item.traction)}</p><h3>Предложение партнёру</h3><p>${e(item.offer)}</p></section>${analysisMarkup(item.analysis,{compact:true})}</div><aside><section class="panel action-panel"><span class="eyebrow">Возможность для сотрудничества</span><h2>${e(item.partnershipType)}</h2><p>Расскажите, чем можете быть полезны и какой следующий шаг предлагаете.</p>${own?'<p class="inline-note">Это ваш проект. Новые заявки появятся в разделе «Отклики».</p><a class="button full" href="#applications">Открыть отклики</a>':application?`${statusBadge(application.status)}<p class="success-text">Вы уже откликнулись на этот проект.</p><a class="button secondary full" href="#applications/sent">Посмотреть отклик</a>`:`<button class="button full" id="apply">Откликнуться ${icon('arrow')}</button>`}<p class="form-footnote">Демо: заявка сохранится только в вашем браузере.</p></section></aside></div></div>`;
  document.querySelector('#apply')?.addEventListener('click',()=>applyDialog(item,routeId));
}
function applyDialog(item,routeId){
  dialog.innerHTML=`<form id="application-form"><div class="dialog-header"><div><p class="eyebrow">Новый контакт</p><h2 id="dialog-title">Откликнуться на проект</h2></div><button type="button" class="icon-button" id="close-dialog" aria-label="Закрыть">${icon('close')}</button></div><p class="lead">${e(item.title)}</p>${field('message','Ваше предложение','',{textarea:true,min:10,max:2000,rows:4,placeholder:'Чем вы можете помочь? Как предлагаете начать сотрудничество?'})}${field('contact','Как с вами связаться','',{min:3,max:150,placeholder:'Почта, телефон или Telegram'})}<p class="inline-note">Демонстрация: сообщение не отправляется реальному получателю.</p><div id="dialog-error"></div><button type="submit" class="button full">Отправить отклик ${icon('arrow')}</button></form>`;
  dialog.showModal();document.querySelector('#close-dialog').onclick=()=>dialog.close();const form=document.querySelector('#application-form');
  form.onsubmit=event=>{event.preventDefault();if(!form.reportValidity())return;const data=formData(form);pending(form,'Сохраняем отклик…',async()=>{try{await api.apply(item.id,data);dialog.close();notify('Отклик сохранён. Он появился в разделе «Отправленные».');if(routeId===token)await detailPage(item.id,routeId);}catch(error){report(error,'#dialog-error');}});};
}
async function applicationsPage(routeId,direction){
  if(direction)applicationFilters.direction=direction;
  const filters={...applicationFilters};const items=await api.listApplications(filters);if(routeId!==token)return;
  const received=filters.direction==='received';
  main.innerHTML=`<div class="container">${pageHeading('От знакомства к сотрудничеству','Отклики',received?'Люди и команды, которым интересно ваше предложение.':'Ваши предложения другим проектам и их текущий статус.')}<div class="applications-toolbar"><div class="tabs" role="group" aria-label="Направление откликов"><button data-direction="received" class="${received?'active':''}" aria-pressed="${received}">Полученные</button><button data-direction="sent" class="${!received?'active':''}" aria-pressed="${!received}">Отправленные</button></div><div class="status-filter"><label for="response-status">Статус</label><select id="response-status"><option value="">Все отклики</option value="PENDING" ${filters.status==='PENDING'?'selected':''}>Ожидают решения</option><option value="ACCEPTED" ${filters.status==='ACCEPTED'?'selected':''}>Принятые</option><option value="REJECTED" ${filters.status==='REJECTED'?'selected':''}>Отклонённые</option></select></div></div><div id="action-error"></div><div class="section-title"><h2>${received?'Полученные':'Отправленные'} <span class="count">${items.length}</span></h2><span class="muted">Контакты доступны в каждом отклике</span></div>${items.length?`<div class="applications-list">${items.map(item=>`<article class="application-card" data-application="${e(item.id)}"><div class="application-person"><span class="person-avatar">${e(item.name.split(' ').map(word=>word[0]).slice(0,2).join(''))}</span><div><h2>${e(received?item.name:item.opportunityTitle)}</h2><p>${e(received?item.company:'Ваш отклик')}</p></div><time datetime="${e(item.createdAt)}">${formatDate(item.createdAt)}</time></div><p class="application-project">К проекту <a href="#opportunity/${e(item.opportunityId)}">${e(item.opportunityTitle)}</a></p><p class="application-message">${e(item.message)}</p><div class="application-contact">${icon('mail')}${e(item.contact)}</div><div class="application-bottom">${statusBadge(item.status)}${received&&item.status==='PENDING'?`<div class="button-row"><button class="button secondary small" data-respond="REJECTED" data-id="${e(item.id)}">Отклонить</button><button class="button small" data-respond="ACCEPTED" data-id="${e(item.id)}">${icon('check')}Принять</button></div>`:''}</div></article>`).join('')}</div>`:empty(received?'Таких откликов пока нет':'Здесь появятся ваши отклики',received?'Выберите другой статус или дождитесь новых предложений.':'Найдите интересный проект в каталоге и предложите сотрудничество.',!received?'<a class="button" href="#catalog">Открыть каталог</a>':'')}</div>`;
  document.querySelectorAll('[data-direction]').forEach(button=>button.onclick=()=>{applicationFilters={direction:button.dataset.direction,status:''};go(`applications/${button.dataset.direction}`);});
  document.querySelector('#response-status').onchange=event=>{applicationFilters.status=event.target.value;route();};
  document.querySelectorAll('[data-respond]').forEach(button=>button.onclick=()=>pending(button.closest('article'),'Сохраняем…',async()=>{try{await api.respond(button.dataset.id,button.dataset.respond);notify(button.dataset.respond==='ACCEPTED'?'Отклик принят. Можно связаться с партнёром.':'Отклик отклонён. Статус сохранён.');if(routeId===token)await applicationsPage(routeId);}catch(error){if(routeId===token)report(error);}}));
}
async function route(){
  const routeId=++token;const [view='business',id,stage]=(location.hash.slice(1)||'business').split('/');
  if(dialog.open)dialog.close();main.innerHTML=loading();window.scrollTo(0,0);
  const nav=view==='opportunity'?'catalog':view;
  document.querySelectorAll('[data-nav]').forEach(link=>{const active=link.dataset.nav===nav;link.classList.toggle('active',active);if(active)link.setAttribute('aria-current','page');else link.removeAttribute('aria-current');});
  try{
    if(!meta){meta=await api.getMeta();document.querySelector('#storage-note').textContent=meta.persistent?'Изменения сохраняются в этом браузере':'Временная сессия: после обновления изменения сбросятся';}if(routeId!==token)return;
    if(view==='business'&&!id)await businessPage(routeId);
    else if(view==='business'&&id==='new')await draftPage(null,routeId);
    else if(view==='business'&&id){const business=await api.getBusiness(id);if(routeId!==token)return;if(business.ownerId!==meta.user.id)throw new Error('Эта карточка принадлежит другому автору. Откройте её в каталоге.');const screens={draft:draftPage,questions:questionsPage,card:editableCardPage,rating:ratingPage,published:publishedPage};await (screens[stage]||screens[stageFor(business)])(id,routeId);}
    else if(view==='catalog')await catalogPage(routeId);
    else if(view==='opportunity'&&id)await detailPage(id,routeId);
    else if(view==='applications')await applicationsPage(routeId,['sent','received'].includes(id)?id:undefined);
    else throw new Error('Страница не найдена. Вернитесь в «Мой бизнес».');
    if(routeId===token){document.title=`${{business:'Мой бизнес',catalog:'Каталог',opportunity:'Карточка проекта',applications:'Отклики'}[view]||'Бизнес-возможности'} — Qadam`;main.focus({preventScroll:true});}
  }catch(error){if(routeId!==token)return;main.innerHTML=`<div class="container">${empty('Не получилось открыть страницу',e(error.message),'<button class="button" id="retry">Попробовать снова</button><a class="button secondary" href="#business">Мой бизнес</a>')}</div>`;document.querySelector('#retry').onclick=route;}
}
document.querySelector('.skip-link').onclick=event=>{event.preventDefault();main.focus();main.scrollIntoView({block:'start'});};
window.addEventListener('hashchange',route);
route();
