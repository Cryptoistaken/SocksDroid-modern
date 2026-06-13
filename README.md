SocksDroid-modern
---
A modernized SOCKS5 client for Android 5.0+ (supporting up to Android 16)

This version features a modernized UI with Material You (Dynamic Colors) and introduces several new features:
- **Profiles**: Save and manage multiple server configurations.
- **Authentication**: Supports SOCKS5 username and password authentication.
- **IPv6 Support**: Option to route IPv6 traffic through the proxy.
- **Per-App Proxy**: Split tunneling to choose which apps use the proxy (or bypass it).
- **Auto Connect**: Automatically connect on device boot.

Most of the JNI code are imported from `shadowsocks-android` project because they have already done most of the work.

### THIS IS NOT A SHADOWSOCKS CLIENT! SOCKS5 IS NOT SHADOWSOCKS!

UDP Forwarding
---
As `tun2socks` does not support UDP associate but has its own implementation of UDP forwarding `badvpn-udpgw`, so it is needed that the udpgw daemon run on remote server to use UDP forwarding.

On remote server

```
badvpn-udpgw --listen-addr 127.0.0.1:7300
```

And set `UDP Gateway` in this app to `127.0.0.1:7300`

DNS
---
If the server does not run `udpgw`, DNS lookups can also be processed in this app.

It makes use of the TCP DNS feature of `pdnsd`. You just set a DNS server that supports TCP DNS in this app, and all DNS requests will be transformed into TCP queries.

Routing
---
The app has an embedded list of non-Chinese IPs. Chinese users can make use of it for the best experience in bypassing GFW.

GFW
---
Note that SOCKS5 is currently blocked by the GFW, which means Chinese users cannot connect to any SOCKS5 servers outside China directly.

But there are still solutions. For example, you can use tools like stunnel to circumvent the firewall.

License
---
This project is licensed under GNU General Public License Version 3 or later.

---

# Русский (Russian)

SocksDroid-modern
---
Модернизированный SOCKS5-клиент для Android 5.0+ (с поддержкой вплоть до Android 16).

Эта версия отличается обновленным пользовательским интерфейсом с поддержкой Material You (Dynamic Colors) и включает несколько новых функций:
- **Профили (Profiles)**: Сохранение и управление несколькими конфигурациями серверов.
- **Аутентификация (Authentication)**: Поддержка аутентификации SOCKS5 по имени пользователя и паролю.
- **Поддержка IPv6**: Возможность маршрутизации IPv6-трафика через прокси.
- **Прокси для приложений (Per-App Proxy)**: Раздельное туннелирование для выбора приложений, которые будут использовать прокси (или обходить его).
- **Автозапуск (Auto Connect)**: Автоматическое подключение при загрузке устройства.

Перенаправление UDP (UDP Forwarding)
---
Так как `tun2socks` не поддерживает UDP Associate, но имеет собственную реализацию перенаправления UDP `badvpn-udpgw`, для работы этой функции необходимо, чтобы на удаленном сервере был запущен демон udpgw.

На удаленном сервере:

```
badvpn-udpgw --listen-addr 127.0.0.1:7300
```

Затем в этом приложении установите `UDP Gateway` (UDP-шлюз) на `127.0.0.1:7300`.

DNS
---
Если сервер не использует `udpgw`, DNS-запросы также могут обрабатываться в этом приложении.

Оно использует функцию TCP DNS программы `pdnsd`. Вам нужно просто указать в приложении DNS-сервер, который поддерживает TCP DNS, и все DNS-запросы будут преобразованы в TCP-запросы.

Маршрутизация (Routing)
---
Приложение содержит встроенный список IP-адресов России. Он нужен, чтобы не копроментировать прокси.

РКН
---
Обратите внимание, что протокол SOCKS5 в настоящее время блокируется РКН.

Но решения есть. [socks-reabilitator](https://github.com/AlexeyRF/Socks-Reabilitator).

Лицензия (License)
---
Этот проект распространяется под лицензией GNU General Public License Version 3 или новее.
