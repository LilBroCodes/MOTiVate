MOTiVate is a plugin created for managing your MOTDs properly.

Should work for all versions that i listed, will backport it to older versions once i have time to. (Probably tomorrow... which is today because it's 00:05)

# How to configure
## config.yml
Look at the template created when launching the plugin for how to configure.
Here's how it works:
- The plugin checks if an incoming request has player data saved
- If not, the plugin removes all messages with require-player-data set to true
- Then the plugin picks a random message out of the ones you have and displays it
Exceptions
- If there is no player data saved and there are no messages that don't require it, an error message will be shown instead of the MOTD.
- If the config is invalid, an error message detailing the error will be shown every time.
## players.yml
Don't touch this file, unless you want to clear player data. All it does is store players' UUIDs based on their hostnames, so that their data can be retrieved when fetching the MOTD.

I am looking to encrypt this later on in some way to prevent hostnames (That are basically the players' IPs) from being stored in plaintext
