
**Team 2, CS 441 - Project**
========================
---


Google Drive folder containing documentation, screenshots, terminal output and picture of all team members can be found at the following link:

https://drive.google.com/drive/folders/1di-_KUJ-CICP2TNjoJpYbLON-Nms67ly?usp=sharing

The source code is uploaded and can be found in this bitbucket repository.



##**Team members**
___

* Pratik Anil Kshirsagar, Graduate, 671863272, pkshir2@uic.edu
* Gurpreet Kaur Chabada, Graduate, 651102968, gchaba2@uic.edu
* Ashwani Khemani, Graduate, 660570981, akhema2@uic.edu
* Dipankar Ghosh, Graduate, 652811050, dghosh6@uic.edu
* Riccardo Pressiani, Graduate, 650547946, rpress4@uic.edu
* Giovanni Agugini Bassi, Graduate, 659013265, gagugi2@uic.edu
* Mayuri Kumari, Graduate, 672159677, mkumar29@uic.edu 



##**Description of the file structure of the repository**
___

* [output](output) : testing results of our application 
* [runningSpark](runningSpark) : files required to setup the spark cluster 
* [Main.java](src/main/java/com/hortonworks/example/Main.java) : code for the monte carlo simulator application 
* [stockData](stockData) :  input data for the simulator collected from the finanice API
* [target](target) :  build folder for the application 
* [README.md](README.md) : readme file for the submission 
* [companies_list.txt](companies_list.txt) : the list of companies used 
* [downloadData.py](downloadData.py) : the script to extract data from the finance API
* [downloadHistoricalData.sh](downloadHistoricalData.sh) : the script to extract data from the finance API
* [pom.xml](pom.xml) :  build file for the application 



##**Instructions for a quick test of our application**
___

The Pi in the **black case** is the **Kubernetes master** (with the hostname riccardo.local). The rest are K8s slaves.
Connect to the pis using ssh. Passwords of all pis are `hypriot`

Listing the commands for every pi below:

```
ssh pirate@riccardo.local
ssh pirate@giovanni.local
ssh pirate@ashwani.local
ssh pirate@dipankar.local
ssh pirate@hallows.local
ssh pirate@pratik.local
ssh pirate@mayuri.local
```


Alternately, we can also ssh using the IPs of the above pis.
We have cloned this repo on the master pi. 



### **Perform the following on Master node:**
```
sudo su
kubeadm reset
kubeadm init --pod-network-cidr 10.244.0.0/16
su pirate
sudo cp /etc/kubernetes/admin.conf $HOME/
sudo chown $(id -u):$(id -g) $HOME/admin.conf
export KUBECONFIG=$HOME/admin.conf
curl -sSL https://rawgit.com/coreos/flannel/v0.7.1/Documentation/kube-flannel-rbac.yml | kubectl create -f -
curl -sSL https://rawgit.com/coreos/flannel/v0.7.1/Documentation/kube-flannel.yml | sed "s/amd64/arm/g" | kubectl create -f -
sudo iptables -P FORWARD ACCEPT
```



### **Perform the following on the slave nodes:**
```
sudo su
kubeadm reset
```


Once it is initialized it will give such a command for slaves to join
```
<kubeadm_join_command_issued_by_master_node>
sudo iptables -P FORWARD ACCEPT
```



###**On the master node, do the following to set up the Spark cluster:**
	
Inside the clone of this repo on the master pi(riccardo) run the following commands: 
```
cd runningSpark
kubectl create -f spark-master.yaml
kubectl get pods
```


Identify the 'spark-master' pod. Enter the shell of the master using:
```
kubectl exec -it <name_of_master_pod> bash
tail -f spark/logs/_(whatever is the name of the only file here)
```


Wait till the screen shows "I HAVE BEEN ELECTED: I AM ALIVE!". Now exit the master shell, and follow the next commands.
```
exit
kubectl create -f spark-master-service.yaml
kubectl create -f spark-worker.yaml
kubectl get pods
```

	
Identify the 'spark-master' pod. Enter the shell of the master using:
```	
kubectl exec -it <name_of_master_pod> bash
tail -f spark/logs/_(whatever is the name of the only file here)
```


Wait till the screen shows "Registered worker..."
	
`spark-submit --properties-file s3.properties --class com.hortonworks.example.Main --master spark://spark-master:7077 --num-executors 4 --driver-memory 1024m --executor-memory 1024m --executor-cores 4 --queue default --deploy-mode cluster --conf spark.eventLog.enabled=true --conf spark.eventLog.dir=file:///eventLogging mc.jar s3a://spark-cloud/input/companies_list.txt s3a://spark-cloud/input/*.csv s3a://spark-cloud/output`
	


Wait till the screen shows "Registering app monte-carlo-var-calculator" and "Launching executor app.. on worker..". Now, exit the master shell, and follow the next commands.
```
exit
kubectl get pods
```


Pick the first worker in the list, copy its name
```
kubectl exec -it <spark_worker_name> bash
cd spark/work
ls
```

	
Will show a "driver" file directory
```
cd <driver..whatever_the_name_is>
tail -f stdout
```

	
You will see the output of our program here! :thumbsup:

To check the output folder specified while running the program please run the following command to download the output directory from s3. The aws cli interface is configured with our credentials. They can be replaced.
```
aws s3 cp s3://spark-cloud/output/ output --recursive
```

This will download the output directory to a folder named 'output' in the directory from which this command is executed.