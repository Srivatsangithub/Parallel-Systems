# Parallel-Systems
### Projects part of Parallel Systems coursework- NC State (Spring 2022)

#### 1. Distributed Training with TensorFlow and Keras

Parallelised the training process of a Keras model over multiple GPU nodes based on synchronous data parallelism, using the tf.distribute API. Performed multi-worker distributed synchronous training using MultiWorkerMirroredStrategy.
_______________________________________________________________________
#### 2. Distributed Testing with PyTorch
Implemented model testing using distributed communication in order to support multiple nodes, performed message passing between GPUs using PyTorch.distributed API.
_______________________________________________________________________
#### 3. TFICF-MPI Implementation
Computed TFICF values for each word in a corpus of documents via an MPI implementation to enable sharing of data of each word to the different worker nodes using non-blocking MPI calls such as MPI_Igatherv, MPI_Isend, and MPI_Irecv. The root rank in the set of processors acts as the master that divides and distributes the documents in the corpus to the worker nodes. The worker nodes communicate with each other to compute the TFICF values of each word.
_______________________________________________________________________
#### 4. TFICF-Spark Implementation
Computed the TFICF value of each word in the corpus of documents provided using Apache Spark and Java. The program is split into three primary jobs namely TF Job, ICF Job, and TF-ICF Job with each phase having its mapping and reduce phase with transformations such as mapToPair, flatMapToPair, reduceByKey, etc.

_______________________________________________________________________
#### 5. TFICF-MapReduce Implementation
Implemented the calculation of TFICF for each word in a corpus of documents using MapReduce. The TFICF calculation has been accomplished via three jobs (Word Count, Document Size and TFICF calculation) where each job has its respective Mapper and Reducer classes.
_______________________________________________________________________
#### 6. CUDA Programming
Computed the integral of a given function (cos() here) by calculating the cumulative area under the curve in a parallel scheme using CUDA programming.

_______________________________________________________________________
#### 7. Point-to-Point Message Latency for Pairs of Nodes
The average round-trip time (RTT) as a function of message size is computed and plotted along with the standard deviation (std. dev) as error bars. The program calculates the point-to-point message latency for four different pairs of nodes (pair, 2 pairs, 3 pairs, 4 pairs) for the following message sizes: 32KB, 64KB, 128KB, 256KB, 512KB, 1MB, and 2MB between Process 0 and Process 1.

_______________________________________________________________________
#### 8. Computing Derivatives in a Parallel Scheme- MPI
Implemented the computation of the derivative of a function (sinx() in this case) in a parallel manner using MPI, data decomposition and finite difference method. Boundary conditions have been communicated using blocking point-to-point communication and non-blocking point-to-point communication.


