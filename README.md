# Authenticator

## Description

A Minecraft plugin to help you set up a Minecraft verification process for a third-party application.
When player issues the command `/verify` on a Minecraft server (1.20.4) an 8-digits code is generated.

The following data (a Ticket) are then inserted into a MySQL database :
- `datetime` (DATETIME) : The date the command was issued at
- `uuid` (VARCHAR(36)) : The player's UUID
- `code` (INT) : The 8-digits code

When a user tries to verify on a third-party application (a website for instance) he will be prompted a code. 
A request to the database attempts to retrieve a Ticket with the given code. If a Ticket is found, the UUID can be used
to associate a Minecraft username to the website account.

![Scheme.png](Scheme.png)

**Note:** It is recommended for the third-party application to define an expiration delay for a Ticket 
(for instance: if the Ticket has been emitted more than 5 minutes ago it will no longer be considered as valid).

## Behavior
- The `/verify` command has a default cooldown of 5 minutes
- There can't be 2 Tickets with the same code in the database
- Database entry that are older than 10 minutes are automatically deleted (TODO)
- All issued tickets are logged in the `tickets.txt` file (TODO)

## TODO

- [ ] Auto deleting old ticket after a certain amount of time (10 minutes)
- [ ] Log every issued tickets in a separated file
- [x] Regenerate the code if it's already found in the database

