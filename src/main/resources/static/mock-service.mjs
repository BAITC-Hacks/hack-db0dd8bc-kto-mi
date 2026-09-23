import {currentUser, categories, cities, partnershipTypes, clarificationQuestions, analysisTemplate, seedState, exampleDraft} from './mock-data.mjs';

export const STORAGE_KEY = 'qadam-business-demo-v1';
const copy = value => structuredClone(value);
const fail = message => { throw new Error(message); };
const clean = (value, name, min = 1, max = 3000) => {
  const text = String(value ?? '').trim();
  if (text.length < min || text.length > max) fail(`${name}: от ${min} до ${max} символов.`);
  return text;
};

// Implements the frontend service contract. All fixtures and demo rules stay in this layer.
export function createMockService({storage = null, delay = 300} = {}) {
  let state = seedState();
  if (storage) {
    try {
      const saved = JSON.parse(storage.getItem(STORAGE_KEY));
      if (saved?.version === 1 && Array.isArray(saved.opportunities) && Array.isArray(saved.applications)) state = saved;
    } catch { /* An unreadable old demo starts with fresh fixtures. */ }
  }
  const wait = () => new Promise(resolve => setTimeout(resolve, delay));
  const query = async read => { await wait(); return copy(read()); };
  const mutate = async write => {
    await wait();
    const next = copy(state), result = write(next);
    if (storage) {
      try { storage.setItem(STORAGE_KEY, JSON.stringify(next)); }
      catch { fail('Не удалось сохранить изменения в браузере. Освободите место или разрешите локальное хранение и повторите действие.'); }
    }
    state = next;
    return copy(result);
  };
  const get = (data, id) => data.opportunities.find(item => item.id === id) || fail('Карточка не найдена. Вернитесь в каталог.');
  const owned = (data, id) => { const item = get(data,id); if (item.ownerId !== currentUser.id) fail('Эту карточку может изменять только её автор.'); return item; };
  const draftFields = input => {
    if (!categories.includes(input.category) || !cities.includes(input.city) || !partnershipTypes.includes(input.partnershipType)) fail('Выберите сферу, город и формат сотрудничества.');
    return {title:clean(input.title,'Название',3,100),description:clean(input.description,'Описание',30,3000),category:input.category,city:input.city,partnershipType:input.partnershipType};
  };
  const uid = prefix => `${prefix}-${globalThis.crypto.randomUUID()}`;
  return {
    getMeta: () => query(() => ({user:currentUser,categories,cities,partnershipTypes,mode:'mock',persistent:!!storage,exampleDraft})),
    listMine: () => query(() => state.opportunities.filter(item => item.ownerId === currentUser.id).sort((a,b) => b.createdAt.localeCompare(a.createdAt))),
    getBusiness: id => query(() => get(state,id)),
    createDraft: input => mutate(data => {
      const item = {...draftFields(input),id:uid('business'),ownerId:currentUser.id,status:'DRAFT',answers:{},analysis:null,createdAt:new Date().toISOString()};
      data.opportunities.unshift(item);return item;
    }),
    updateDraft: (id,input) => mutate(data => {const item=owned(data,id);if(item.status!=='DRAFT')fail('Описание уже передано в работу. Измените готовую карточку.');Object.assign(item,draftFields(input));return item;}),
    getQuestions: id => mutate(data => {const item=owned(data,id);if(item.status==='DRAFT')item.status='QUESTIONS';return clarificationQuestions;}),
    saveAnswers: (id,answers) => mutate(data => {
      const item=owned(data,id);if(!['QUESTIONS','CARD','RATED'].includes(item.status))fail('Сначала откройте вопросы к черновику.');
      item.answers=Object.fromEntries(clarificationQuestions.map(q=>[q.id,clean(answers[q.id],q.title,10,2000)]));
      Object.assign(item,item.answers,{tagline:item.description.split(/[.!?]/)[0].slice(0,140),status:'CARD',analysis:null});return item;
    }),
    updateCard: (id,input) => mutate(data => {
      const item=owned(data,id);if(!['CARD','RATED'].includes(item.status))fail('Редактирование доступно до публикации.');
      Object.assign(item,draftFields(input),{tagline:clean(input.tagline,'Краткое предложение',10,160),audience:clean(input.audience,'Клиенты',10,2000),traction:clean(input.traction,'Результаты',10,2000),offer:clean(input.offer,'Предложение партнёру',10,2000),status:'CARD',analysis:null});return item;
    }),
    analyze: id => mutate(data => {
      const item=owned(data,id);if(!['CARD','RATED'].includes(item.status))fail('Сначала подготовьте карточку.');
      const breakdown=analysisTemplate.categories.map(category=>({...category,score:Math.round(category.weight*Math.min(.96,.52+String(item[category.key]||'').length/500))}));
      item.analysis={score:breakdown.reduce((sum,part)=>sum+part.score,0),breakdown,explanation:analysisTemplate.explanation,strengths:copy(analysisTemplate.strengths),risks:copy(analysisTemplate.risks),recommendations:copy(analysisTemplate.recommendations)};
      item.status='RATED';return item;
    }),
    publish: id => mutate(data => {const item=owned(data,id);if(item.status==='PUBLISHED')return item;if(item.status!=='RATED'||!item.analysis)fail('Сначала получите актуальную оценку карточки.');item.status='PUBLISHED';item.publishedAt=new Date().toISOString();return item;}),
    listCatalog: ({search='',category='',city='',partnershipType='',minimumScore=0,sort='rating'}={}) => query(() => {
      const term=search.toLocaleLowerCase('ru');
      return state.opportunities.filter(item=>item.status==='PUBLISHED'&&(!category||item.category===category)&&(!city||item.city===city)&&(!partnershipType||item.partnershipType===partnershipType)&&(item.analysis?.score||0)>=Number(minimumScore)&&`${item.title} ${item.tagline} ${item.description}`.toLocaleLowerCase('ru').includes(term))
        .sort((a,b)=>sort==='newest'?b.createdAt.localeCompare(a.createdAt):sort==='rating-asc'?a.analysis.score-b.analysis.score:b.analysis.score-a.analysis.score);
    }),
    apply: (id,input) => mutate(data => {
      const item=get(data,id);if(item.status!=='PUBLISHED')fail('Карточка ещё не опубликована.');if(item.ownerId===currentUser.id)fail('Вы автор этой карточки. Откликнитесь на другой проект.');
      if(data.applications.some(application=>application.opportunityId===id&&application.applicantId===currentUser.id))fail('Вы уже откликнулись на этот проект. Откройте раздел «Отправленные».');
      const application={id:uid('application'),opportunityId:id,applicantId:currentUser.id,recipientId:item.ownerId,name:currentUser.name,company:currentUser.company,message:clean(input.message,'Сообщение',10,2000),contact:clean(input.contact,'Контакт',3,150),status:'PENDING',createdAt:new Date().toISOString()};
      data.applications.unshift(application);return application;
    }),
    listApplications: ({direction='received',status=''}={}) => query(() => state.applications.filter(item=>(direction==='sent'?item.applicantId===currentUser.id:item.recipientId===currentUser.id)&&(!status||item.status===status)).map(item=>({...item,opportunityTitle:get(state,item.opportunityId).title})).sort((a,b)=>b.createdAt.localeCompare(a.createdAt))),
    respond: (id,status) => mutate(data => {
      const application=data.applications.find(item=>item.id===id)||fail('Отклик не найден.');
      if(application.recipientId!==currentUser.id)fail('Решение принимает автор проекта.');if(application.status!=='PENDING')fail('Этот отклик уже обработан. Обновите список.');if(!['ACCEPTED','REJECTED'].includes(status))fail('Выберите «Принять» или «Отклонить».');application.status=status;return application;
    }),
  };
}
