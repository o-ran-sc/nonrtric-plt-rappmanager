# Rome Threshold Control Demo Steps

## Prerequisites
1. Full access to the osc cosmos lab smo VM - including port forwarding to localhost
2. Access to the kubernetes cluster
3. Full access to the native@srv6.rrail.cosmos-lab.or vm for the DU/RU setup
4. Access the threshold control rApp.
5. Access to the Rome Demo Postman Collection

## See InfluxDB
The Influx db port should be forwarded to localhost. You can access the dash with the appropriate credentials:

```text
username: user
password: password
```

There should be a dashboard prepared called "Rome Demo", that will monitor the traffic we are interested in.
The charts there monitor connected devices and throughput.

Set the GUI up so it is visible in the demo.

## See RU in SDNC
The SDNC port should be forwarded to localhost. You can access the SDNC GUI with the appropriate credentials:

```text
username: user
password: password
```

In the "connect" page of the sdnc web gui, you should see the RU, DU and CU.
The DU should be marked disconnected at the moment. Make sure the device statuses are visible in the SDNC GUI.

The RU should already be connected.

1. Navigate to the congiguration tab and click on the RU.
2. Click on the "ietf-hardware", the "hardware".
3. Click on Component
4. Client on the benetel RU line.
5. Now at the bottom, you should see the "energy-saving-enabled" attribute set to "false".
6. This will change later and should be visible in demo.

## Access the Power Consumption Dashboard
We need access to srv5.rrail.cosmos-lab.org for this and port 3000 needs to be forwarded to localhost.
ssh password for the server is native101
It is a Grafana dashboard that shows the power consumption of the RU.
The dashboard is preconfigured to show the power consumption of the RU.
The dashboard should be visible in the demo.
The credentials should not be required but add if appropriate

```text
username: user
password: password
```

## Start the devices
These are the instructions for actually starting the DU, o1 adaptors and UE.

You need to have access to -->  ssh native@srv6.rrail.cosmos-lab.or

Change directory to `cd /home/native/sa_fhi/7.2`

### To start the setup, DU/CU, Core Network and O1 adaptors

```
/home/native/sa_fhi_7.2/start-setup.sh
```

The output should be

```
Starting core services...
WARN[0000] No services to build                         
[+] up 6/6
 ✔ Network oai-core     Created                                                          0.0s 
 ✔ Container mysql      Created                                                          0.1s 
 ✔ Container oai-ext-dn Created                                                          0.1s 
 ✔ Container oai-amf    Created                                                          0.1s 
 ✔ Container oai-smf    Created                                                          0.0s 
 ✔ Container oai-upf    Created                                                          0.1s 
Waiting for core services to become healthy...
Waiting for oai-amf...
oai-amf is healthy
Waiting for oai-smf...
oai-smf is healthy
Waiting for oai-upf...
oai-upf is healthy
Waiting for mysql...
mysql is healthy
Waiting for oai-ext-dn...
oai-ext-dn is healthy
All core services are healthy. Starting duranta-cu...
WARN[0000] No services to build                         
[+] up 5/5
 ✔ Network oai-f1       Created                                                          0.0s 
 ✔ Network oai-o1-net   Created                                                          0.0s 
 ✔ Container mysql      Running                                                          0.0s 
 ✔ Container oai-amf    Healthy                                                          0.6s 
 ✔ Container duranta-cu Created                                                          0.1s 
Starting O1 CU Adaptor...
WARN[0000] No services to build                         
[+] up 5/5
 ✔ Network smo-net         Created                                                       0.0s 
 ✔ Container mysql         Running                                                       0.0s 
 ✔ Container oai-amf       Healthy                                                       0.6s 
 ✔ Container duranta-cu    Healthy                                                       1.1s 
 ✔ Container o1-adapter-cu Created                                                       0.1s 
Start DU and O1 DU Adaptor...
WARN[0000] No services to build                         
WARN[0000] Found orphan containers ([o1-adapter-cu duranta-cu oai-upf oai-smf oai-amf mysql oai-ext-dn]) for this project. If you removed or renamed this service in your compose file, you can run this command with the --remove-orphans flag to clean it up. 
[+] up 3/3
 ✔ Container dpdk-init     Exited                                                        4.3s 
 ✔ Container duranta-du    Healthy                                                      14.9s 
 ✔ Container o1-adapter-du Created                                                       0.0s 
Done.

```

Check that all the RAN counters should be good
because if they are not good which can happen sometimes because of the RU we would need to reboot the RU

To check the counters, all the counters should be aligned PUSCHX and PRACHX should have same values for all four antenna

```
docker logs duranta-du
```

The logs will look like below

```
[HW]     [o-du 0][rx 4988928 pps   86016 kbps 3558144][tx 7394436 pps  127488 kbps 4717971][Total Msgs_Rcvd 4988928]
[HW]     [o_du0][pusch0 1069056 prach0  178176]
[HW]     [o_du0][pusch1 1069056 prach1  178176]
[HW]     [o_du0][pusch2 1069056 prach2  178176]
[HW]     [o_du0][pusch3 1069056 prach3  178176]
```

If the counters are not aligned like below, then you need to reboot benetel else skip the below steps

```
[HW]     [o-du 0][rx 4988928 pps   86016 kbps 3558144][tx 7394436 pps  127488 kbps 4717971][Total Msgs_Rcvd 4988928]
[HW]     [o_du0][pusch0 1069056 prach0  178106]
[HW]     [o_du0][pusch1 1069056 prach1  178146]
[HW]     [o_du0][pusch2 1069056 prach2  178176]
[HW]     [o_du0][pusch3 1069056 prach3  178173]
```

[OPTIONAL only to fix benetel]

```
#Stop the setup
./stop.sh
# SSH to benetel
ssh root@10.3.12.20
reboot
#once reboot keep pinging to 10.3.12.20, once it is up again ssh
# Check the RU stats and wait for it to be ready
tail -f /tmp/logs/radio_status
#wait for the below line
[INFO] Radio bringup complete
 16:19:17 up 5 min,  load average: 0.21, 0.47, 0.26 
```

Now you can restart

```
./start-setup.sh
```

**At this point the DU should be connected on the SDNC screen. We should start
to see data coming from influxdb but no traffic or connected devices showing.
Monitoring period is 2 minutes by default so wait for that.**

### To connect the UE

```
ssh root@srv1-in3.sb1.cosmos-lab.org
./start-ue.sh
# To disconnect laterif wewant to disconnect
./stop-ue.sh
# If the UE does not connect you need to do stop and then again start
# Start Traffic 
ping -I wwan0 -i 0.1 12.1.1.1
iperf3 -c 192.168.71.135 -B 12.1.1.2 -u -b 50M --bidir -t 300
```

When we start the UE, we should, in about 2 minutes start to see connected devices be reflected in the influxdb dashboard.
Once it is clear that these start to show up, we should start the traffic.

After a short amount of time, we should start to see traffic flowing in influxdb and it 
should be reflected in the power consumption dashboard.

## Deploy rApp
This requires access to the Rome Demo Postman Collection

1. Ensure access to the rappmanager service by running the Get Rapps request.
2. In the Onboard Threshold Control rApp, make sure the rapp csar is added to the request body.
3. Execute the Post Onboard Threshold Control rApp request
4. Verify that the rapp is successfully onboarded by running the Get Rapps request.
5. Prime the rApp.
6. Ensure there are no rApp instances by running the Get Rapp Instances request.
7. Now create an instance by running the Create Rapp Instance Threshold Control request.
8. Make sure the instance is created successfully by running the Get Rapp Instances request.
9. Deploy the instance by running the Deploy Rapp Instance Threshold Control request.

### Check rApp Logs
Now, if we check the rApp logs, we will see that it is processing the traffic - taking the data from influxdb.
It will monitor the throughput to see if the use of the RU drops below the threshold.
If it does, it will send a message to the SDNC to put the RU in energy saving mode.

It will not do this yet because there is still throughput on the RU and DU.

### Trigger the energy saving mode
Now, we want to trigger the energy saving mode.
We can do this by stopping the UE - this should cause the rApp threshold.
control logic to run and put the RU in energy saving mode.

1. Stop the UE - ./stop-ue.sh
2. Wait for the threshold control logic to run and put the RU in energy saving mode. This should take about 2 minutes.
3. Check the SDNC to see that the RU is now in energy saving mode.
4. You should now be able to see the power consumption drop significantly.
5. We should also see the traffic and the UEs connected in the influxdb dashboard drop.

This energy saving mode will last for 2 minutes, at which point the rApp will disable the energy saving mode again.
Then we will see power consumption return to normal and traffic resume.
We can repeat the steps again to show the same thing happening again.

## Troubleshooting
### Clean Up
If there is a case of a failed deployment that cannot be cleaned up via the API, we can use the following steps.
When cleaning up, it is best to carry out both ACM Cleanup and SME Cleanup - detailed below.

#### ACM Cleanup
Consult the postman collection under the "Cleanup ACM" directory for the ACM cleanup steps.
1. Run `Get All Templates ACM-Direct`
2. Run `Get Template ACM-Direct`
3. Run `Get All Instances ACM-Direct`
4. Run `Get Instance ACM-Direct`
5. The above will populate the postman collection variables with the template and instance IDs.
6. Run `Undeploy Instance ACM-Direct` to undeploy the instance.
   Wait for the pods to undeploy (if they are stuck or leftover).
7. Run `Delete Instance ACM-Direct` to delete the instance.
8. Run `Delete Template ACM-Direct` to delete the template.
9. That should conclude the ACM cleanup.

#### SME Cleanup
SME cleanup requires some manual steps.
1. Delete all the kong services and routes. Put this in some "script.sh" file and run it. The place where you run it
   should have access to the cluster.
   ```bash
      SERVICEMANAGER_POD=$(kubectl get pods -o custom-columns=NAME:.metadata.name -l app.kubernetes.io/name=servicemanager --no-headers -n nonrtric)
      if [[ -n $SERVICEMANAGER_POD ]]; then
      kubectl exec $SERVICEMANAGER_POD -n nonrtric -- ./kongclearup
      else
      echo "Error - Servicemanager pod not found, didn't delete Kong routes and services for ServiceManager."
      fi

   ```
2. Once the above has been run, we must restart some pods.
   ```bash
   kubectl delete pod -l app.kubernetes.io/name=servicemanager -n nonrtric
   kubectl delete pod -l app.kubernetes.io/name=rappmanager -n nonrtric
   kubectl delete pod -l app.kubernetes.io/name=capifcore -n nonrtric
   ```
3. Wait for these pods to come up again to a `Running` state.
4. Now we need to add some preloaded SME configurations.
    1. In the `it/dep` repository, navigate to the `nonrtric/servicemanager-preload` directory.
    2. Preload some of the nonrtric services by running the following command:
       ```bash
       ./servicemanager-preload.sh config-nonrtric.yaml
       ```
    3. Preload the SMO services by running the following command:
       ```bash
       ./servicemanager-preload.sh config-smo.yaml
       ```
5. This should conclude the SME cleanup. Then we can attempt to redeploy the rApp again with whatever
   changes we made to fix the issues.
