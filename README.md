# CaAuth

Плагин для Paper (Minecraft 26.3+), который привязывает игровой аккаунт к Discord и Telegram и работает как второй фактор для входа на сервер. Через ботов игрок может подтверждать вход, менять пароль, привязывать твинков и управлять скинами.

## Возможности

- **Привязка аккаунта** к Discord и/или Telegram через одноразовый код (`/binding code`).
- **Подтверждение входа** через бота: раз в заданный интервал бот просит подтвердить, что в игру заходит владелец. Пока вход не подтверждён, игрок заморожен.
- **Смена пароля** через бота (бот выдаёт новый случайный пароль).
- **Твинки**: можно привязать несколько дополнительных аккаунтов к одному владельцу.
- **Скины** (нужен SkinsRestorer): 7 профилей скинов, установка по нику, ссылке или PNG-файлу, загруженному в бота. Меню в игре (`/skins`) и в ботах.
- **Роли в Discord**: автовыдача роли привязанным, роли за донат-группы и персонал (по LuckPerms), роль «активного игрока» по отыгранному времени (Plan), автосмена ника на сервере Discord.
- **Банк в ботах** (интеграция с CalistoBanking).
- **Защита кодов привязки**: ограничение числа неверных попыток и временная блокировка.
- **Прокси для ботов**: SOCKS5, SOCKS4 или HTTP, в том числе для WebSocket-соединения Discord.
- **Режим Velocity**: работа вместе с tiAuth и PicoLimbo через плагин CaAuthVelocity.
- **PlaceholderAPI**: счётчик онлайна.

## Требования

- Paper с `api-version` 26.3, Java 25.
- Необязательные зависимости: LuckPerms, Plan, PlaceholderAPI, SuperVanish / PremiumVanish, SkinsRestorer, CalistoBanking.

## Установка

1. Скачайте `CaAuth-<версия>.jar` со страницы [Releases](https://github.com/shelzi133/CaAuth/releases) и положите в папку `plugins`.
2. Запустите сервер, чтобы создались конфиги.
3. Откройте `plugins/CaAuth/auth/auth-config.yml`, включите нужного бота и впишите токен (см. ниже).
4. Перезапустите сервер или выполните `/caauth reload`.

Все библиотеки уже встроены в jar.

## Настройка

Файлы в `plugins/CaAuth/`:

| Файл | Назначение |
|---|---|
| `config.yml` | Включение модулей (auth, skins) и режим Velocity |
| `auth/auth-config.yml` | Боты, прокси, подтверждение входа, коды, пароль, твинки |
| `auth/auth-messages.yml` | Сообщения авторизации |
| `bots/roles.yml` | Роли и ник на сервере Discord |
| `bots/messages.yml` | Сообщения скинов в ботах |
| `skins/skins-config.yml` | Меню `/skins`, тексты, лимиты |

В каждом файле у параметров есть комментарии.

### Модули

В `config.yml` модули включаются и выключаются по отдельности. Если модуль выключен, его кнопок в ботах нет.

```yaml
module:
  auth:
    enabled: true
  skins:
    enabled: true
  tiauth:
    enabled: true
  banking:
    enabled: true
```

- `skins`: скины (нужен SkinsRestorer).
- `tiauth`: смена пароля и твинки (работает вместе с режимом Velocity).
- `banking`: банк и переводы (нужен CalistoBanking).

### Боты

```yaml
bots:
  discord:
    enabled: true
    token: "ТОКЕН_DISCORD_БОТА"
    guild-id: "ID_СЕРВЕРА"
  telegram:
    enabled: true
    token: "ТОКЕН_TELEGRAM_БОТА"
```

`guild-id` нужен для проверки участников сервера и выдачи ролей.

### Подтверждение входа

```yaml
bots:
  login-confirmation:
    enabled: true
    interval-hours: 6
    timeout-seconds: 60
```

### Прокси для ботов

Нужен, если Discord или Telegram недоступны из региона сервера.

```yaml
proxy:
  enabled: true
  type-bot: all
  type: SOCKS5
  host: "1.2.3.4"
  port: 1080
  username: ""
  password: ""
```

`type-bot` принимает `telegram`, `ds` или `all`. Для Discord прокси применяется и к REST-запросам, и к WebSocket-соединению.

### Роли Discord

В `bots/roles.yml` включаются роли привязанных игроков, донат-группы и персонал (по группам LuckPerms), роль активного игрока по времени из Plan и автосмена ника. Для каждой группы указывается id роли на сервере Discord.

### Velocity

Если вход на сервере идёт через Velocity (tiAuth + PicoLimbo), включите `module.auth.velocity.enabled` в `config.yml`. API поднимается на `host:port`, секретный ключ `secret` должен совпадать с ключом в `config.yml` плагина CaAuthVelocity. Кик при отвязке и смена пароля отправляются на указанные прокси.

## Команды

| Команда | Описание | Право |
|---|---|---|
| `/binding` (`/bind`, `/verify`) | Статус привязки | — |
| `/binding code` | Получить код для привязки в боте | — |
| `/unbind <игрок>` | Отвязать чужой аккаунт | `caauth.unbind` |
| `/activityplayer [игрок]` | Выдать или снять роль активного игрока | `caauth.activityplayer.self`, `caauth.activityplayer.other` |
| `/skins` | Меню скинов | — |
| `/caauth reload` (`/caau`) | Перезагрузить конфиги | `caauth.admin` |
| `/caauth info <игрок>` | Информация о привязках игрока | `caauth.admin` |
| `/caauth ds start\|stop\|restart\|info\|online-reload` | Управление Discord-ботом | `caauth.admin` |
| `/caauth tg start\|stop\|restart\|info` | Управление Telegram-ботом | `caauth.admin` |

Права `caauth.*` по умолчанию выдаются операторам.
