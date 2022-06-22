#Single Author info:
#srames22 Srivatsan Ramesh
p2: p2.cu
	nvcc p2.cu -o p2 -O3 -lm -Wno-deprecated-gpu-targets -I${CUDA_HOME}/include -L${CUDA_HOME}/lib64