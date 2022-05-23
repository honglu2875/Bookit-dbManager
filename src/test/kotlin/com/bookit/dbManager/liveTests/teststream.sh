#!/bin/bash
./testAddScheduleType.sh $1
./testAddLockedSlot.sh $1
./testForceSync.sh $1
