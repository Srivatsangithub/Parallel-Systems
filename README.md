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

_______________________________________________________________________
#### 5. TFICF-MapReduce Implementation

_______________________________________________________________________
