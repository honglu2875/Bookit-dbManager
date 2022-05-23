#!/bin/bash
echo "----------------------/api/backend/add_schedule_type-------------------"
./testAddScheduleType.sh $1
echo "========= add dummy data, force sync========="
./testAddLockedSlot.sh $1
./testForceSync.sh $1
echo "----------------------/api/backend/{token}/change_available_type---------------"
echo "Changing the available interval from default to [100,1000]. Verify it in the get requests later."
./testChangeAvailableTime.sh $1
echo "----------------------/api/backend/get_user-------------------"
./testGetUser.sh $1
echo "----------------------/api/backend/{token}--------------------"
./testGetScheduleType.sh $1
echo "---------------delete /api/backend/{token}--------------------"
./testDeleteScheduleType.sh $1
echo "========= try getting schedule type again. Should be either an error or a type added before the test========"
./testGetScheduleType.sh $1
