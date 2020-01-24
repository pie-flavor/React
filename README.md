# React

This is a little game that is played in chat. A bit of JSON text is shown to all players every so often, with some hover text. The first person to correctly type the hover text wins, and it is shown in chat who won and how long it took.

If the server is not currently running (for example: a singleplayer world that is not open to LAN), this plugin is not active.

### Commands

This plugin has no direct commands, but it does support `/sponge plugins reload`. When run, the configuration is reloaded, taking effect immediately.

### Configuration

There are three Text blocks, `text{}`, `prefix{}`, and `suffix{}`. `prefix{}` and `suffix{}` go before and after `text` respectively when creating the message, and only `text{}` has the hover text on it. `prefix{}` and `suffix{}` are also optional.

There is also the `words[]` block. `words[]` is a list of things to put on the hover text. Which one to use is chosen randomly each time. Each entry in `words[]` does not have to be a single word, and can be multiple words. Note that any word using any characters other than letters must have quotes around it.

`delay` is the number of seconds between new words being selected. It is also the delay after starting or reloading before  a new game is selected. 

`min-players` is the number of players that is required for the game to start. Set to 0 to disable.

The `rewards{}` block handles extra handling when someone wins. Inside `rewards{}`, there is `commands[]`, which is a list of commands that will get run. If a command starts with `*`, it will be run by the console; otherwise, it will be run by the player who won. In all commands, `$winner` will get replaced by the name of the player who won. Also in `rewards{}` is `economy{}`, which has two keys, `currency` and `amount`. `currency` is the name of the currency to use, as defined by the economy plugin. All economy plugins have a default currency, so this is optional. `reward` is the amount of money to award the player with.

### Changelog

Note: The latest version of this plugin is compatible with both API 5 and API 4.1.

1.0.0: If you hover over the version number, it'll tell you.

1.1.0: Added support for rewards.

1.2.0: Added a minimum players count, and fixed configs not version checking.

1.2.1: Fixed rewards being applied on every chat message.

1.2.2: Fixed the config not generating properly.

1.2.3: Fixed a bug where the config would not load unless `words[]` was edited.

1.2.4: Fixed `min-players` acting like a maximum
