# VBans
### Ban your players.

## What?
VBans is a complete tool to handle player banning on your Velocity server.<br>
It contains:
- Banning
- Tempbanning
- Kicking
- Ban/Kick History
- Checks the players override permission even if the player is offline (if luckperms is installed)

It saves to History to a mysql server.

## How to install
1. Drop the jar file (found in releases) in to the plugin folder.
2. Start the server
3. Stop the server
4. go to plugins/vbans/config.toml and change the mysql connection details
5. Start the server

## Commands
|Command|What is does|Permissions|
|-------|------------|-----------|
|/ban \<player> [reason]|Bans a player permanently|VBans.ban and VBans.ban.reason|
|/tempban \<player> <time> [reason]|Bans a player for a specific time|VBans.temp and VBans.temp.reason|
|/kick \<player> [reason]|Kicks a player|VBans.kick and VBans.kick.reason|
|/banhistory <player>|Shows the ban/kick history of a player|VBans.history and VBans.history.seeDeleted|
|/delban <player> [ban id]|Deletes the players active ban or the selected ban out of the history (if the history reader does not have seeDeleted permissions|VBans.delete|
|/reduceban <player> [time]|Reduces the players ban to a given time (from ban begin on). If no time is given the player will be unbanned directly. This will not delete the ban out of his history|VBans.reduce|
| |Prevents the player from being kicked/banned|VBans.prevent|
| |Lets the player receive messages when a player was banned|VBans.bannedBroadcast|

## Difference between /delban and /reduce
I developed these commands to get used in different situations.<br>
/delban should only be used if the server made an mistake by banning the player (e.g. The player could prove that he was not hacking)<br>
/reduce or /unban should be used if the server forgives the player (e.g. the player wrote a ban appeal)

## See who has been banned
If you want to see who has been banned. The only way at the moment is to look at the databse with a statement like:
```mysql
SELECT username AS "Banned User", 
       Reason, 
       From_unixtime(until) AS "Banned until", 
       issuedat != until    AS "Was Ban", 
       (until = -1 
              || until > Unix_timestamp()) AS "Still banned" 
FROM   ban_bans, 
       ban_namecache 
WHERE  ban_bans.user = ban_namecache.user 
AND    purged IS NULL;
```
## End
Please report any issue you find.<br>
If you have any problems or you are missing a feature please contact me.
