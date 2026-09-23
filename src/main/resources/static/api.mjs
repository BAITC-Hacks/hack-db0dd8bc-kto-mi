import {createMockService} from './mock-service.mjs';

/**
 * FRONTEND CONTRACT — provisional until the business controllers/DTOs arrive.
 * UI imports only this facade. Replace the implementation here with a real adapter.
 * Required operations and view models:
 * getMeta() -> {user, categories, cities, partnershipTypes, mode, persistent}
 * listMine(), getBusiness(id), createDraft(fields), updateDraft(id, fields)
 * getQuestions(id) -> [{id, title, hint}]
 * saveAnswers(id, {[questionId]: string}) -> Business
 * updateCard(id, fields), analyze(id), publish(id) -> Business
 * listCatalog({search, category, city, partnershipType, minimumScore, sort}) -> Business[]
 * apply(businessId, {message, contact}) -> Application
 * listApplications({direction, status}) -> Application[]
 * respond(applicationId, 'ACCEPTED'|'REJECTED') -> Application
 * Business: id, ownerId, title, description, category, city, partnershipType,
 * tagline, audience, traction, offer, status, answers, analysis, createdAt.
 * Analysis: score, explanation, breakdown[{title,score,weight,detail}],
 * strengths[], risks[], recommendations[{title,text}].
 * Application: id, opportunityId, opportunityTitle, applicantId, recipientId,
 * name, company, message, contact, status, createdAt.
 * Statuses: DRAFT -> QUESTIONS -> CARD -> RATED -> PUBLISHED.
 * Real adapter must map server DTOs to these view models and normalize errors.
 * Intentionally no guessed URLs and no silent fallback from a failing real API to mocks.
 */
let storage = null;
try { storage = globalThis.localStorage; } catch { /* In-memory demo remains usable. */ }
export const api = createMockService({storage});
