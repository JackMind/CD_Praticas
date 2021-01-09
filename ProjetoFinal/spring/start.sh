#!/bin/sh

sessionSpread="spread"
sessionDb="db"
sessionServer="server1"
sessionConfig="config"

if [ "$1" == "stop" ]
then
	tmux kill-session -t $sessionSpread
	tmux kill-session -t $sessionDb
	tmux kill-session -t $sessionServer
	tmux kill-session -t $sessionConfig
else
	tmux new -d -s $sessionSpread
	tmux send-keys -t $sessionSpread "spread -c simple.spread.conf" C-m

	tmux new -d -s $sessionDb
	tmux send-keys -t $sessionDb "sudo docker-compose -f docker-compose.yml up" C-m

	tmux new -d -s $sessionConfig
	tmux send-keys -t $sessionConfig "sh configStart.sh" C-m

	sleep 5

	tmux new -d -s $sessionServer
	tmux send-keys -t $sessionServer "sh 1startServer.sh" C-m
fi