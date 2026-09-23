package com.qadam.service;

import com.qadam.dto.TaskCard;
import com.qadam.model.Industry;
import com.qadam.model.ProposalStatus;
import com.qadam.model.TaskStatus;

import java.util.List;

/**
 * Synthetic demo content in Russian: no real companies and no personal data (contacts use example.com).
 * Published tasks cover all four rating levels; drafts have different completeness.
 */
public final class DemoData {

    public record DemoTask(Industry industry, TaskStatus status, String draftText, TaskCard card) {
    }

    public record DemoTeam(String name, List<String> interests, List<String> skills, List<String> technologies,
                           int points) {
    }

    /**
     * @param task index in {@link #TASKS}
     * @param team index in {@link #TEAMS}
     */
    public record DemoProposal(int task, int team, String idea, String plan, String duration, String prototypeUrl,
                               ProposalStatus status, int confirmedMilestones) {
    }

    public static final List<DemoTask> TASKS = List.of(
            // 0 — published, PRIORITY (100)
            new DemoTask(Industry.RETAIL, TaskStatus.PUBLISHED,
                    "Мы сеть продуктовых магазинов у дома. Хотим понять, какие товары заканчиваются на полках "
                            + "раньше, чем приходит поставка, и заказывать их вовремя.",
                    new TaskCard(
                            "Прогноз пустых полок в магазинах у дома",
                            "Сеть из 25 продуктовых магазинов у дома в спальных районах города.",
                            "Популярные товары заканчиваются до следующей поставки, покупатели уходят к конкурентам.",
                            "Управляющие магазинами и отдел закупок, который формирует заказы поставщикам.",
                            "Обезличенные продажи по чекам за 18 месяцев и график поставок в CSV.",
                            "Срок — 8 недель. Решение должно работать на обычном ноутбуке, без платных облачных сервисов.",
                            "Прототип дашборда с прогнозом остатков на 7 дней и списком товаров под риском.",
                            "Доля дней с пустой полкой по топ-100 товарам снижается на 30% в пилотных магазинах.",
                            "Руководитель отдела закупок, retail-demo@example.com",
                            "Онлайн-созвон раз в неделю и общий чат для быстрых вопросов.")),
            // 1 — published, READY (80): no users, no constraints
            new DemoTask(Industry.LOGISTICS, TaskStatus.PUBLISHED,
                    "У нас курьерская служба, маршруты составляем вручную. Нужно ускорить доставку.",
                    new TaskCard(
                            "Оптимизация маршрутов курьеров",
                            "Городская курьерская служба, 40 курьеров, около 1500 доставок в день.",
                            "Диспетчеры составляют маршруты вручную, курьеры тратят много времени на холостые переезды.",
                            "",
                            "Обезличенная история заказов за 6 месяцев: адреса в виде координат, время доставки.",
                            "",
                            "Сервис, который по списку заказов предлагает маршруты для каждого курьера.",
                            "Средний пробег курьера за смену сокращается на 15% на исторических данных.",
                            "Операционный директор, logistics-demo@example.com",
                            "Встречи в офисе раз в две недели, между встречами — переписка по почте.")),
            // 2 — published, WORKING (62): criteria without numbers, no users/constraints/contact/format
            new DemoTask(Industry.EDUCATION, TaskStatus.PUBLISHED,
                    "Мы частная школа. Ученики забывают про домашние задания, хотим напоминания.",
                    new TaskCard(
                            "Напоминания о домашних заданиях",
                            "Частная школа с углублённым изучением математики, 5–11 классы.",
                            "Ученики часто забывают о сроках домашних заданий, учителя тратят время на напоминания.",
                            "",
                            "Расписание уроков и шаблон электронного журнала без персональных данных учеников.",
                            "",
                            "Прототип мобильного приложения или чат-бота с напоминаниями о заданиях.",
                            "Ученики реже пропускают сроки сдачи домашних заданий.",
                            "",
                            "")),
            // 3 — published, DRAFT (25): shown in the catalog with the «требует уточнения» note
            new DemoTask(Industry.HEALTHCARE, TaskStatus.PUBLISHED,
                    "Клиника. Пациенты долго ждут на телефоне, хотим что-то с этим сделать.",
                    new TaskCard(
                            "Разгрузить регистратуру клиники",
                            "Многопрофильная клиника, регистратура принимает звонки пациентов.",
                            "Пациенты долго ждут ответа по телефону, чтобы записаться на приём.",
                            "",
                            "",
                            "",
                            "",
                            "",
                            "clinic-demo@example.com",
                            "")),
            // 4 — published, READY (85): no constraints, no interaction format
            new DemoTask(Industry.FINANCE, TaskStatus.PUBLISHED,
                    "Микрофинансовая организация. Хотим заранее видеть клиентов, которые могут просрочить платёж.",
                    new TaskCard(
                            "Ранние сигналы просрочки платежей",
                            "Микрофинансовая организация, выдаёт небольшие займы малому бизнесу.",
                            "Просрочки обнаруживаются слишком поздно, когда клиенту уже сложно помочь с реструктуризацией.",
                            "Кредитные менеджеры, которые сопровождают клиентов после выдачи займа.",
                            "Синтетический набор данных о платежах 5000 заёмщиков, подготовленный по нашей структуре.",
                            "",
                            "Модель, которая ранжирует клиентов по риску просрочки, и понятный отчёт для менеджеров.",
                            "Модель находит не меньше 70% будущих просрочек за 30 дней до платежа.",
                            "Руководитель риск-отдела, finance-demo@example.com",
                            "")),
            // 5 — draft, 10: only context
            new DemoTask(Industry.AGRICULTURE, TaskStatus.DRAFT,
                    "Мы фермерское хозяйство, выращиваем овощи в теплицах. Хотим что-то автоматизировать.",
                    new TaskCard(
                            "Автоматизация в тепличном хозяйстве",
                            "Фермерское хозяйство, выращиваем овощи в теплицах.",
                            "", "", "", "", "", "", "", "")),
            // 6 — draft, WORKING (40): context, need, data
            new DemoTask(Industry.MANUFACTURING, TaskStatus.DRAFT,
                    "Мебельная фабрика. Много брака на станке раскроя, есть журнал брака в Excel.",
                    new TaskCard(
                            "Причины брака на участке раскроя",
                            "Мебельная фабрика полного цикла, участок раскроя плит.",
                            "На станке раскроя много брака, причины никто системно не анализировал.",
                            "",
                            "Журнал брака в Excel за последний год с датами, сменами и типами дефектов.",
                            "", "", "", "", "")),
            // 7 — draft, WORKING (55): context, need, result, measurable criteria, contact
            new DemoTask(Industry.MEDIA, TaskStatus.DRAFT,
                    "Региональное онлайн-издание. Хотим, чтобы читатели дольше оставались на сайте.",
                    new TaskCard(
                            "Рекомендации статей для читателей",
                            "Региональное онлайн-издание о городских новостях и событиях.",
                            "Читатели уходят после одной статьи, глубина просмотра низкая.",
                            "",
                            "",
                            "",
                            "Блок «Читайте также» с рекомендациями похожих статей.",
                            "Глубина просмотра растёт с 1,3 до 1,8 страницы за визит.",
                            "media-demo@example.com",
                            "")),
            // 8 — draft, READY (85): no users, no interaction format
            new DemoTask(Industry.HORECA, TaskStatus.DRAFT,
                    "Небольшой отель. Хотим понять, как менять цены на номера в зависимости от сезона.",
                    new TaskCard(
                            "Сезонные цены на номера отеля",
                            "Небольшой городской отель на 30 номеров.",
                            "Цены на номера меняем интуитивно, в сезон теряем выручку, вне сезона пустуют номера.",
                            "",
                            "Обезличенная история бронирований за 3 года и цены конкурентов из открытых источников.",
                            "Без доступа к системе бронирования, работа только с выгрузками. Срок — 6 недель.",
                            "Таблица или простой сервис с рекомендуемой ценой на каждую дату.",
                            "Средняя загрузка номеров вне сезона растёт на 10 процентных пунктов.",
                            "Управляющий отелем, hotel-demo@example.com",
                            "")),
            // 9 — draft, 0: only a title
            new DemoTask(Industry.IT, TaskStatus.DRAFT,
                    "Нужен бот для сотрудников.",
                    new TaskCard("Бот для сотрудников", "", "", "", "", "", "", "", "", "")));

    public static final List<DemoTeam> TEAMS = List.of(
            new DemoTeam("Data Wizards",
                    List.of("аналитика", "розничная торговля", "маркетинг"),
                    List.of("анализ данных", "визуализация", "прогнозирование"),
                    List.of("Python", "SQL", "Power BI"), 20),
            new DemoTeam("Маршрут",
                    List.of("логистика", "оптимизация"),
                    List.of("математическое моделирование", "backend-разработка"),
                    List.of("Python", "OR-Tools", "PostgreSQL"), 10),
            new DemoTeam("EdTech Lab",
                    List.of("образование", "мобильные приложения"),
                    List.of("UX-дизайн", "frontend-разработка"),
                    List.of("React", "Figma", "TypeScript"), 0),
            new DemoTeam("Health Bots",
                    List.of("здравоохранение", "чат-боты"),
                    List.of("backend-разработка", "обработка текста"),
                    List.of("Java", "Spring", "Telegram API"), 0),
            new DemoTeam("FinSight",
                    List.of("финансы", "риски"),
                    List.of("машинное обучение", "анализ данных"),
                    List.of("Python", "scikit-learn", "Excel"), 30));

    public static final List<DemoProposal> PROPOSALS = List.of(
            new DemoProposal(0, 0,
                    "Прогнозируем остатки по каждому товару и подсвечиваем риск пустой полки.",
                    "1. Разбор данных. 2. Базовый прогноз продаж. 3. Дашборд. 4. Пилот в 3 магазинах.",
                    "8 недель", "https://example.com/shelf-prototype", ProposalStatus.ACCEPTED, 1),
            new DemoProposal(0, 3,
                    "Чат-бот, который присылает управляющему список товаров для дозаказа.",
                    "1. Сценарии бота. 2. Интеграция с выгрузкой остатков. 3. Тест с управляющими.",
                    "6 недель", null, ProposalStatus.REJECTED, 0),
            new DemoProposal(1, 1,
                    "Решаем задачу маршрутизации с окнами доставки и сравниваем с ручными маршрутами.",
                    "1. Подготовка данных. 2. Модель маршрутизации. 3. Сервис с картой. 4. Сравнение пробега.",
                    "7 недель", "https://example.com/routes-demo", ProposalStatus.ACCEPTED, 0),
            new DemoProposal(2, 2,
                    "Мобильное приложение с напоминаниями и еженедельным планом заданий.",
                    "1. Интервью с учителями. 2. Прототип в Figma. 3. Приложение на React Native.",
                    "5 недель", null, ProposalStatus.PENDING, 0),
            new DemoProposal(4, 4,
                    "Модель риска просрочки с объяснением факторов для каждого клиента.",
                    "1. Анализ данных. 2. Базовая модель. 3. Отчёт для менеджеров. 4. Проверка на отложенной выборке.",
                    "6 недель", null, ProposalStatus.PENDING, 0));

    private DemoData() {
    }
}
