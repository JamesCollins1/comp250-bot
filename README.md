# comp250-bot

This is the base repository for COMP250 assignment 1, task 2 (MicroRTS bot).

## [Tournament leaderboard](http://comp250.falmouth.games)

## Getting started

For detailed instructions, watch [this video](https://youtu.be/pvzp8c4nZaU).

* Fork this repository
* Go to Settings -> Webhooks -> Add Webhook
* Enter the following details:
  - Payload URL: http://comp250.falmouth.games/hook
  - Content type: application/json
  - Secret: leave blank
  - Which events: just the push event
  - Active: yes
  - Click Add Webhook
* Clone your repository, making sure to also clone submodules (e.g. "Recursive" option in TortoiseGit, `--recurse-submodules` switch on command line)
* Pushes to the master branch will automatically trigger an upload to the tournament server
