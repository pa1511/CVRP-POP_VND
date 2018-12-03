# CVRP-POP_VND
Capacitated vehicle routing problem solved using population based variable neighborhood descent.

## Short introduction
The main idea behing this project is to comapre the effectiveness of the basic VND and population based VND algorithm and not to produce the best solutions for the presented CVRP problem.
We used the Augerat 1995 P CVRP dataset available at [link](http://vrp.atd-lab.inf.puc-rio.br/index.php/en/)

## Strategies used
In our experiments we use the following neighborhoods: 
* merges two routes into one 
* swap stations between two routes
* move the station from one route to another
Note that after every change the route is re-optimized using a simple TSP solver.

In our experiments we use the following neighbor selection strategies:
* best improving neighbor
* random improving neighbor
* first improving neighbor

## Results
Through the experiments made in this project we show that using population based VND especially with neighborhood generation strategy shuffling can produce noticeably better results than just a basic implementation of VND. 

### Random strategy results (Hower to se the image title): 
![](images/random.png "Average route length reduction")
![](images/shuffle-random.png "Average route length reduction with shuffling")
![](images/random-count-plot.png "Number of instances solved better/equal/worse than VND")
![](images/random-shuffle-count-plot.png "Number of instances solved (using shuffling) better/equal/worse than VND")
![](images/box-random.png "Route length reduction")
![](images/box-random-shuffle.png "Route length reduction with shuffling")

### First strategy results (Hower to se the image title): 
![](images/first.png "Average route length reduction")
![](images/shuffle-first.png "Average route length reduction with shuffling")
![](images/first-count-plot.png "Number of instances solved better/equal/worse than VND")
![](images/first-shuffle-count-plot.png "Number of instances solved (using shuffling) better/equal/worse than VND")
![](images/box-first.png "Route length reduction")
![](images/box-first-shuffle.png "Route length reduction with shuffling")

### Best strategy results (Hower to se the image title): 
![](images/best.png "Average route length reduction")
![](images/shuffle-best.png "Average route length reduction with shuffling")
![](images/best-count-plot.png "Number of instances solved better/equal/worse than VND")
![](images/best-shuffle-count-plot.png "Number of instances solved (using shuffling) better/equal/worse than VND")
![](images/box-best.png "Route length reduction")
![](images/box-best-shuffle.png "Route length reduction with shuffling")

#### Technical note
This project uses the [Heuristic-algorithms](https://github.com/pa1511/Heuristic-algorithms) project as the basis of its implementation. 
