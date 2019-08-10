# CNV-2019 
 
This project was done in collaboration with @mariojcc and @LuisLoureiro for the cloud computation an virtualization course during my Informatics and Computer Engineering Masters specializing in Intelligent Systems and Data Analysis and Processing at IST.
 
## Introduction
The goal of the CCV project is to design and develop an elastic cluster of web servers that is able to execute a simple e-science related function: to ﬁnd the maximum value on simpliﬁed height-maps (maps), representing elevations with colours, on-demand, by executing a set of search/exploration algorithms (serving as a demonstrator of CPU-intensive processing). The system will receive a stream of web requests from users. Each request is for ﬁnding the maximum on a given map, providing the coordinates of the start position and a search rectangle within the height-map. In the end, it displays the height-map and the computed path (in gray-scale) to reach the maximum, using hill-climbing.
 
 
## System's components
 
### Web Servers
The HillClimbing@Cloud web servers are system virtual machines running an off-the-shelf Java-based web
server application on top of Linux. The web server application will serve a single web page that receives
a HTTP request providing the necessary information, i.e., the height-map to analyze, the coordinates
(xS, yS) for the start position, the top-left (x0, y0) and bottom-right (x1, y1) corners of the active search
rectangle within the height-map, and the strategy that they use for hill-climbing (e.g., BFS, DFS, A*).
The page serving the requests will perform the solving online and, once it is complete, reply to the
web request with a confirmation, and if successful by drawing the search path leading to the maximum
overlaid on the height-map


### Load Balancer
The load balancer is the only entry point into the system: it receives a sequence of web requests and
selects one of the active web server cluster nodes to handle each of the requests. In a first phase, this
job was performed by an off-the-shelf load balancer such as those available at Amazon AWS. Later in
the project, a more advanced load balancer was designed. It used metrics data obtained in earlier
requests, stored in the Metrics Storage System, to pick the best web server node to handle a request.
The load balancer can estimate the complexity, load and approximate duration of a request, based
on the request’s parameters combined with data previously stored in the MSS, that may be periodically
or continuously updated by the MSS. The load balancer may know which servers are busy, how many
and what requests they are currently handling, what the parameters of those requests are, their current
progress, and how much work is left taking into account the estimate that was calculated when the
request arrived.


### Auto-Scaler
The auto-scaler is in charge of collecting system performance metrics and, based on
them, adjusting the number of active web servers. Initially an Amazon AWS Autoscaling group that adaptively decided how many
web server nodes was used. In a posterior phase a custom AS was created with custom rules to better suit the system's needs.


### Metrics Storage System
The metrics storage system uses the available data storage
mechanisms at AWS to store web server performance metrics relating to requests. These help
the load balancer choose the most appropriate web server to handle requests.

