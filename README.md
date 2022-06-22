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
