# Energy Saving rApp Demo
The code, helm chart and rApp specification contained in this directory is used to deploy the Energy Saving rApp demo.
The demo is designed to showcase the capabilities of the rApp platform in managing energy consumption in a network.
The instructions below describe how to:

- Deploy the demo in a Kubernetes cluster.
- Create and deploy the Energy Saving rApp.
- Confirm that the Energy Saving rApp is running and managing energy consumption in the network.
- Undeploy the Energy Saving rApp.
- Troubleshoot any issues that may arise during the deployment or undeployment process.

## Prerequisites
- A Kubernetes cluster, various versions and sims supported - see [here](https://github.com/o-ran-sc/it-dep/tree/master/smo-install).
- See prerequisites [here](https://github.com/o-ran-sc/it-dep/tree/master/smo-install)
- A clone of this repository.
- Postman.

## Deployment Steps

### SMO Installation
For a complete guide on the installation of the SMO rApp platform,
please follow the instructions [here](https://github.com/o-ran-sc/it-dep/tree/master/smo-install).

### Energy Saving rApp Deployment Preparation
1. Navigate to the `es-demo-rapp` directory.
2. To generate the rApp csar and helm chart, run the following command:
   ```bash
   ./generate.sh rapp-energy-saving
   ```
3. Make sure to expose the rappmanager service in the `nonrtric` namespace. This is done by running the following command:
   ```bash
   kubectl expose service rappmanager --type=NodePort --name=rappmanager-exposed -n nonrtric
   ```
4. You will be using the postman collection provided in the main directory of this repository to create the rApp.
5. Open Postman and import the `rapp-energy-saving.postman_collection.json` file.
6. It is important to note the collection-level variables in the postman collection.
    1. REMOTE-IP: This is the location the rappmanager service is deployed/exposed to.
    2. PORT: This should be the port where the rappmanager is exposed.
    3. rappId: This is the ID of the rApp you will be creating. It should be unique and can be any alphanumeric string.
    4. rappInstanceId: It will be automatically populated when you create the rApp instance.
    5. PREIDCT_PORT: This is the port where the prediction service is running. It should be set to `40077` by default.

### rApp Deployment
1. In Postman, select the `Onboard ES rApp` request from the collection. Send this request.
2. Then run the `Get Rapps` request to confirm that the rApp has been onboarded successfully.
3. Run the `Prime rApp` request to prime the rApp.
4. Run `Get All Rapp Instances` to confirm that no rApp instance have been created.
5. Run the `Create Rapp Instance ES` request to create an instance of the rApp.
6. Run the `Get Rapp Instance` request to confirm that the rApp instance has been created successfully.
7. Run the `Deploy Rapp Instance` to trigger installation of the rApp instance.
8. The above deployment can take time, so you can run the `Get Rapp Instance` request to check the status of the rApp instance.
9. You can also monitor the kubernetes pods in the `nonrtric` namespace to see if the rApp instance helm charts are being deployed.

### Confirmation
1. To confirm successful running of the demo energy saving rApp, we can look at the kubernetes logs of the pod.
   ```bash
   kubectl logs -f app.kubernetes.io/name=energy-saving-rapp -n nonrtric
   ```
2. You should see logs indicating that the rApp is running and managing energy consumption in the network.
3. For example, you should see:
    1. logs of cells being turned off and on based on the energy consumption in the network.
    2. Predictions of energy consumption based on the current network load being returned to make poweer management decisions.

## Undeployment
Undeployment of the rApp can also be done with the Postman collection.
1. Run the `Undeploy Rapp Instance` request to undeploy the rApp instance. This takes some time.
2. Run the `Get Rapp Instance` request to confirm that the rApp instance has been undeployed successfully.
3. Run the `Delete Rapp Instance` request to delete the rApp instance.
4. Run the `Get All Rapp Instances` request to confirm that no rApp instances are present.
5. Run the `Deprime rApp` request to deprime the rApp.
6. Run the `Delete ES Rapp` request to delete the rApp.
7. This should conclude the undeployment of the Energy Saving rApp.

## Troubleshooting
If you encounter any issues during the deployment or undeployment of the rApp, please check the following:
1. Is deployment of the pods stuck in ACM?
    - Check the logs of the ACM pod and the kubernetes participant in the `onap` namespace.
    - Is there any indication that install of the pods failed?
    - Check the deployment status of the pods
2. Is there an issue with the SME part of the installation?
    - Check the logs of the servicemanager pod in the `nonrtric` namespace.
    - Is there any indication that the SME is not able to communicate with the rApp Manager?

### Clean Up
If there is a case of a failed deployment that cannot be cleaned up via the API, we can use the following steps.
When cleaning up, it is best to carry out both ACM Cleanup and SME Cleanup - detailed below.

#### ACM Cleanup
Consult the postman collection under the "Cleanup" directory for the ACM cleanup steps.
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