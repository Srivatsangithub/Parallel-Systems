// Single Author info:
// srames22 Srivatsan Ramesh
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/time.h> // To utilize gettimeofday
#include <math.h> // For sqrt and pow functions
#include <mpi.h> // to utilize the MPI API

int main(int argc, char** argv) 
{   int message_size_arr[] = {8192, 16384, 32768, 65536, 131072, 262144, 524288}; // array of different messages sizes in bytes 
                                                                                  // eg. (32kb * 1024 bytes)/4 bytes = 8192 int elements 
    struct timeval start_time;  // struct objs to access the time in microsecond in the struct
    struct timeval end_time;    // using gettimeofday
    int start;
    int end;
        
    int i,j,k;
    int rank, size;
    MPI_Status status;      // MPI_Status struct object 

    MPI_Init(&argc, &argv);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);       // rank gives the current process
    MPI_Comm_size(MPI_COMM_WORLD, &size);       // size has the total number of processes

    for(j=0; j< 7; j++)
    {   int diff = 0;
        int diff_arr[10] = {0,0,0,0,0,0,0,0,0,0};
        int send_message[message_size_arr[j]];  // message buffer to be sent 
        int recv_message[message_size_arr[j]];  // and received 
        int count = message_size_arr[j]; 

    for(i = 0; i<10; i++)       // since we need to calculate average round trip time over 10 iterations
    {

        if (size > 1)           // to check for number of processes more than 1
        {
            if ( rank %2 == 0 )     // for even processes
            {   
                if(rank == 0 && i == 0)     // to skip the first iteration's time calculation as warmup time 
                {                           // could skew average round trip time
                    MPI_Send(&send_message, count, MPI_INT, rank + 1, 100, MPI_COMM_WORLD);
                    MPI_Recv(&recv_message, count, MPI_INT, rank + 1, 200, MPI_COMM_WORLD, &status);

                }
                else if(rank == 0)          // Since we need to calculate time only for one pair of processes we choose Process 0 and 1
                {
                    gettimeofday(&start_time, NULL);
                    start = start_time.tv_usec;
                    MPI_Send(&send_message, count, MPI_INT, rank + 1, 100, MPI_COMM_WORLD);
                    MPI_Recv(&recv_message, count, MPI_INT, rank + 1, 200, MPI_COMM_WORLD, &status);
                    gettimeofday(&end_time, NULL);  
                    end = end_time.tv_usec;

                    diff = diff + (end - start);    // calculating the difference of end time and start time in each iteration 
                    diff_arr[i] = (end - start);    // appending the round trip time in the array for std. dev calculation
                
                }
                else    // for all other even processes
                {
                    MPI_Send(&send_message, count, MPI_INT, rank + 1, 100, MPI_COMM_WORLD);
                    MPI_Recv(&recv_message, count, MPI_INT, rank + 1, 200, MPI_COMM_WORLD, &status);
                }


            
            } 
            else     // all odd processes receive a message and send to the previous processes
            {   
                MPI_Recv(&recv_message, count, MPI_INT, rank - 1, 100, MPI_COMM_WORLD, &status);
                MPI_Send(&send_message, count, MPI_INT, rank - 1, 200, MPI_COMM_WORLD);
            }

        

        }

    }
    float sum_squared_diff = 0;
    float rtt_avg = diff/9;     // diff contains the sums of round trip times for 10 iterations
    float std_dev = 0;
    for(k = 1; k<10; k++)
        {
            std_dev += pow(diff_arr[k] - rtt_avg, 2);   // find the power of each value in diff_arr - avg. round trip time and apply summation

        }

    std_dev = sqrt(std_dev/9); // To calculate Std. dev

    if(rank == 0)
    {       

        printf("%d %d %f\n", (message_size_arr[j] * 4)/1024, diff/9, std_dev);  // to make sure only one process prints the size, round trip time for 10 iterations, std. dev
    }
}


    MPI_Finalize();
    return 0;
}
