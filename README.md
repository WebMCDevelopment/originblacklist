<div align="center">
  <h1>OriginBlacklist<br><img src="https://github.com/WebMCDevelopment/originblacklist/actions/workflows/gradle.yml/badge.svg"></h1>
  <b><a>An eaglercraft client blacklisting plugin</a></b>
</div>

<h2>Features</h2>

- [x] Origin based blacklisting
- [x] Client brand based blacklisting
- [x] Username based blacklisting
- [x] IP based blacklisting
- [x] Kick message customization
- [x] Blacklist MOTD customization
- [x] MiniMessage and legacy formattings supported
- [x] Plugin update checker
- [x] Send blacklist logs to a webhook
- [x] Ingame blacklist management command
- [x] Reverse blacklist (whitelist)
- [ ] Subscribe to an auto-updating blacklist

<h2>Changes from v1</h2>

- [x] Modular multi-platform support
- [x] JSON5 based configuration

<h2>Download</h2>
The latest release can be found at <b><a href="https://github.com/WebMCDevelopment/originblacklist/releases/latest/">https://github.com/WebMCDevelopment/originblacklist/releases/latest/</a></b>

<h2>Building</h2>

```sh
$ git clone https://github.com/WebMCDevelopment/originblacklist
$ cd originblacklist
$ gradle wrapper
$ ./gradlew shadowJar
```
