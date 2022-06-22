#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <cuda.h>

/* first grid point */
#define   XI              0.0
/* last grid point */
#define   XF              M_PI


/* function declarations */
//double     fn(double);
void        print_function_data(int, double*, double*, double*);
int         main(int, char**);

// cos function in the device 
__device__ double cos_func( double  a )         
{
        return cos(a);
}
// Kernel definition with __global__ keyword
__global__ void integral_func(int N, double *x, double *y, double *inf_arr, double *d_area, double h1)
{       
        // To identify each thread uniquely
        int i = blockIdx.x * blockDim.x + threadIdx.x;         
        
        // To restrict the threads that need to function and to avoid looking into out of bounds memory spaces
        if (i < N)                                      
        {
                y[i] = cos_func(x[i]); // Each thread will perform function call on its respective i element of the xc array
        }
        __syncthreads();  // So that y array is filled before any race between threads occur
        
        if(i < N && i!=0)               
        {
                d_area[i] = (y[i] + y[i-1])/2*h1; // For all threads except thread 0 since y[0-1] since y[-1] is out of bounds
        }
        
        else if(i == 0 )  // have to specify i == 0 and so that threads greater than N don't start executing 
        {       
                d_area[i] = (y[i] + cos_func(0))/2*h1;
        }
        __syncthreads(); 

        //if(i < N && i!=0)
        if(i < N) // Copying d_area values into inf_arr
        {       
                inf_arr[i] = d_area[i];         
        }
        // else if(i == 0)
        // {
        //         inf_arr[0] = d_area[0];
        // }
        __syncthreads();
        if(i < N && i!=0) 
        {
                for(int j = i; j > 0; j--) // Each thread computes from its respective index to 0 the cumulative sum and puts it in inf_arr[i]
                {
                        inf_arr[i] = inf_arr[i] + d_area[j-1];  
                } 
        }
}

int main (int argc, char *argv[])
{
        int NGRID;
        if(argc > 1)
            NGRID = atoi(argv[1]);
        else 
        {
                printf("Please specify the number of grid points.\n");
                exit(0);
        }
        //loop index
        int     i;
        double  h;

        double *inf = (double *)malloc(sizeof(double) * (NGRID) );
        double  *xc = (double *)malloc(sizeof(double)* (NGRID + 1));
        double  *yc = (double*)malloc(sizeof(double) * (NGRID));
        double *new_xc =(double*) malloc((NGRID) * sizeof(double));

        // GPU variables memory allocation
        double *d_xc, *d_yc, *d_inf, *d_area;
        cudaMalloc(&d_xc, NGRID*sizeof(double));
        cudaMalloc(&d_yc, NGRID*sizeof(double));
        cudaMalloc(&d_inf, NGRID*sizeof(double));
        //cudaMalloc(&d_inf, NGRID*sizeof(double));
        cudaMalloc(&d_area, NGRID*sizeof(double));

        //construct grid of x axis
        for (i = 1; i <= NGRID ; i++)
        {
                xc[i] = XI + (XF - XI) * (double)(i - 1)/(double)(NGRID - 1);
        }

        // To remove the extra index before passing it to the device
        for(i = 0; i < NGRID; i++)
        {
                new_xc[i] = xc[i+1];
        }
        //int  imin, imax;  
        //imin = 1;
        //imax = NGRID;
        // get the y value of the origin function, yc array is used for output
        // should not use for computing on GPU
        //for( i = imin; i <= imax; i++ )
        //{
        //        yc[i] = fn(xc[i]);
        //}

        // Deep copy of host array to be passed to device variable
        cudaMemcpy(d_xc, new_xc , NGRID*sizeof(double), cudaMemcpyHostToDevice); // To copy memory of alloted space into device global memory 

        //inf[0] = 0.0;
        h = (XF - XI) / (NGRID - 1);

        // Computing execution configuration
        int numOfThreads = 512;         // Setting maximum number of values
        int numOfBlocks = NGRID/numOfThreads + (NGRID%numOfThreads == 0? 0:1);  // Dynamically determining the number of blocks.

        // Kernel function call
        integral_func<<<numOfBlocks, numOfThreads>>>(NGRID, d_xc, d_yc, d_inf, d_area, h);      
        
        // Copying data back to host
        cudaMemcpy(yc, d_yc, NGRID*sizeof(double), cudaMemcpyDeviceToHost); 
        cudaMemcpy(inf, d_inf, NGRID*sizeof(double), cudaMemcpyDeviceToHost);
        
        // you should parallel the following computation workload on GPU
        //for(i = 1 ; i <= NGRID; ++i){
        //    area = ( fn(xc[i]) + fn(xc[i-1]) ) / 2 * h; 
        //    inf[i] = inf[i-1] + area;
        //}
        
        print_function_data(NGRID, &xc[1], &yc[0], &inf[0]);

        //free allocated memory 
        cudaFree(d_xc);
        cudaFree(d_yc);
        cudaFree(d_inf);
        free(xc);
        free(yc);
        free(inf);

        return 0;
}

//prints out the function and its derivative to a file
void print_function_data(int np, double *x, double *y, double *dydx)
{
        int   i;

        char filename[1024];
        sprintf(filename, "fn-%d.dat", np);

        FILE *fp = fopen(filename, "w");

        for(i = 0; i < np; i++)
        {
                fprintf(fp, "%f %f %f\n", x[i], y[i], dydx[i]);
        }

        fclose(fp);
}
