# TeleVip LSPosed

<p>
  <img src="https://img.shields.io/badge/Platform-Android-green">
  <img src="https://img.shields.io/badge/Framework-LSPosed-blue">
  <img src="https://img.shields.io/badge/License-GPL--3.0-orange">
</p>

A powerful LSPosed module that adds advanced customization features to Telegram clients.

## ✨ Features

### Privacy
- Hide "Seen" status in:
    - Private chats
    - Channels and Groups
- Hide "Typing..." indicator
- Hide online status
- Hide phone number
- Hide story view status
- Show deleted messages
- Prevent deletion of secret media

### Media & Stories
- Save protected stories to gallery
- Save voice messages
- Enable secret media
- Save message edit history

### Telegram Modifications
- Remove content saving restrictions
- Disable stories
- Hide pinned messages
- Disable channel swipe
- Disable profile swipe
- Disable update notifications
- Disable number rounding

### Performance
- Boost Telegram download speed

### Premium
- Enable Local Premium


> More features are available but not listed here.


# Version compatibility

TeleVip no longer depends on a specific Telegram version for clients that ship unobfuscated
`org.telegram.*` classes (official Telegram, Beta, Web, Plus, iMe, Forkgram, ...):

- hooks match the exact signature first and fall back to the closest compatible one when Telegram
  only appends a parameter to a method;
- TL classes are looked up under their old and new homes (`TLRPC$...` <-> `tl.TL_*$...`);
- a hook that cannot be resolved is skipped and logged, the other features keep working.

Forks that ship an R8 obfuscation map (Cherrygram, Nekogram, Telegraph, ...) still need the mapping
in `assets/clients` regenerated for each of their builds. The versions in the list below are only the
ones the maintainers tested.

# Build

Push the repo to GitHub: **Actions → Build APK** builds the APK automatically and attaches it to the
run as the `TeleVip-apk` artifact (push a tag like `v3.6.4` and it is also attached to a Release).
Locally: `./gradlew :app:assembleDebug` (JDK 17, Android SDK 36).

By default the APK is signed with a throw-away debug key, so a later build has a different signature
and Android will refuse to update over it (uninstall first). To keep a stable signature add these
repository secrets (Settings → Secrets and variables → Actions): `KEYSTORE_BASE64`
(`base64 -w0 my.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

# 📱 Supported Clients

| Client | Version |
|---|---|
| Telegram | 12.8.3 (69222) |
| Telegram Beta | 12.9.0 (69579) |
| Telegram Web | 12.8.3 (69229) |
| TG Connect | 11.13.1 (11130109) |
| Plus Messenger | 12.8.1.0 (22350) |
| Nagram | 12.8.1 (1239) |
| NagramX | 12.8.1-2bcd1bd (1253) |
| Nagram XF | 12.7.3 (1245) |
| Nekogram | 12.8.1 (69160) |
| Cherrygram | 12.8.1 (69160) |
| Nicegram | 1.55.0 (2139) |
| iMe | 12.8.1 (12080102) |
| iMe Direct | 12.8.1 (12080109) |
| X Plus | 12.0.1 (61669) |
| ForkClient | 12.8.4.0 (691908) |
| ForkClient Beta | 12.8.4.0 (691909) |
| Skygram | 10.20.6 (40639) |
| Teegra | 10.3.2 (41469) |
| Telegraph | 12.8.1.1 (69172) |
| Telega | 2.4.3 (107) |
| Momogram | 12.6.4 |
| Forkgram Classic | 12.8.10.0 |
| Turrit | 1.8.9.9.5 |


# 📢 Updates

All TeleVip updates are published on Telegram:

➡️ https://t.me/t_l0_e


# ⚠️ Warning
> This module is intended for educational purposes only. Its use may result in issues with your Telegram account, including the risk of banning or suspension. Use it at your own risk.


# 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**.

See the [LICENSE](./LICENSE) file for more information.


# Credits

Partially based on:

- [Re-Telegram](https://github.com/Sakion-Team/Re-Telegram).


Developed by **@mustafa1dev**