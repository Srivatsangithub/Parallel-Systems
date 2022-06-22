// Single Author Info:
// srames22 Srivatsan Ramesh

#include<stdio.h>
#include<string.h>
#include<stdlib.h>
#include<dirent.h>
#include<math.h>
#include <mpi.h>

#define MAX_WORDS_IN_CORPUS 32
#define MAX_FILEPATH_LENGTH 16
#define MAX_WORD_LENGTH 16
#define MAX_DOCUMENT_NAME_LENGTH 8
#define MAX_STRING_LENGTH 64

typedef char word_document_str[MAX_STRING_LENGTH];

typedef struct o {
	char word[32];
	char document[8];
	int wordCount;
	int docSize;
	int numDocs;
	int numDocsWithWord;
} obj;

typedef struct w {
	char word[32];
	int numDocsWithWord;
	int currDoc;
} u_w;

static int myCompare (const void * a, const void * b)
{
    return strcmp (a, b);
}

int main(int argc , char *argv[]){
	DIR* files;
	struct dirent* file;
	int i,j;
	int numDocs = 0, docSize, contains;
	char filename[MAX_FILEPATH_LENGTH], word[MAX_WORD_LENGTH], document[MAX_DOCUMENT_NAME_LENGTH];
	
	// Will hold all TFICF objects for all documents
	obj TFICF[MAX_WORDS_IN_CORPUS];
	int TF_idx = 0;
	
	// Will hold all unique words in the corpus and the number of documents with that word
	u_w unique_words[MAX_WORDS_IN_CORPUS];
	int uw_idx = 0;

	// Used in MPI_Get_Address to set up the MPI_Datatype for the struct that we are going to be used in communication
	struct w w1;

	// Will hold the final strings that will be printed out
	word_document_str strings[MAX_WORDS_IN_CORPUS];

	// MPI Initialization
	int numProcs, rank;
	MPI_Init(&argc, &argv);
	MPI_Comm_size(MPI_COMM_WORLD, &numProcs);
	MPI_Comm_rank(MPI_COMM_WORLD, &rank);
	
	//Count numDocs
	if((files = opendir("input")) == NULL){
		printf("Directory failed to open\n");
		exit(1);
	}	// numDocs is going to be computed by all the ranks 
	while((file = readdir(files))!= NULL){
		// On linux/Unix we don't want current and parent directories
		if(!strcmp(file->d_name, "."))	 continue;
		if(!strcmp(file->d_name, "..")) continue;
		numDocs++;
	}

	// Used to keep track of the starting index of each document for each rank
	int start_i_for_rank_initial = 1;
	// Array to keep track of the starting value of each document for each rank
	int start_i_for_rank[numProcs - 1];
	int number_of_docs_per_rank;
	// Array to stores the number of docs per rank if it is different for each rank
	int number_of_docs_per_rank_array[numProcs - 1];


	// Logic: Have an array that keeps track of the number of documents each processor must handle and send that value to each processor.
		
	// Finding the number of documents per rank (for sure) hence excluding 0th/ root rank
	number_of_docs_per_rank = numDocs/ (numProcs - 1);

	// To check if there are extra documents
	int total_extra_docs = numDocs %  (numProcs - 1);
	
	// Equally divisible so send number_of_docs_per_rank
	if(total_extra_docs == 0)
	{
		MPI_Bcast(&number_of_docs_per_rank, 1, MPI_INT, 0, MPI_COMM_WORLD);
	}
	// When number of docs in corpus is equally divisble with the number of processors: an array with the start number of each document for that rank is sent to the respective rank
	if(total_extra_docs == 0 && rank == 0)
	{

		for(int rank_index = 0; rank_index < numProcs - 1; rank_index++)
		{
			start_i_for_rank[rank_index] = start_i_for_rank_initial;
			start_i_for_rank_initial +=number_of_docs_per_rank;
			MPI_Send(&start_i_for_rank[rank_index], 1, MPI_INT, rank_index + 1, 0, MPI_COMM_WORLD);
		}
	}

	// If the number of documents are not evenly divisible with the number of ranks
	else if(rank==0 && total_extra_docs!=0)
	{
		// Looping through the ranks and assigning equal number of documents to it
		for(int i = 0; i < numProcs - 1; i++ )
		{
			number_of_docs_per_rank_array[i] = number_of_docs_per_rank;
		}

		// Handling the extra documents case
		for (int i = 0; i < total_extra_docs; i++)
		{
			number_of_docs_per_rank_array[i] += 1;
		}
		// To send the starting document and the number of documents each rank is going to handle and compute tficf values for
		for(int rank_index = 0; rank_index < numProcs - 1; rank_index++)
		{
			start_i_for_rank[rank_index] = start_i_for_rank_initial;
			start_i_for_rank_initial += number_of_docs_per_rank_array[rank_index];
			MPI_Send(&start_i_for_rank[rank_index], 1, MPI_INT, rank_index + 1, 0, MPI_COMM_WORLD);
			MPI_Send(&number_of_docs_per_rank_array[rank_index], 1, MPI_INT, rank_index + 1, 1, MPI_COMM_WORLD);
		}
	}
	

	// Receiving appropriate start points and number of documents to read from root node
	if(rank!=0)
	{	
		
		// Evenly Divisible
		if(numDocs %  (numProcs - 1) == 0)
		{
			MPI_Recv(&start_i_for_rank_initial, 1, MPI_INT, 0, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
			//printf("rank %d : %d\t", rank, start_i_for_rank_initial);
		}
		else
		{	// Receiving when there are extra documents for a few ranks i.e number of documents are not evenly divisible by the number of ranks
			MPI_Recv(&start_i_for_rank_initial, 1, MPI_INT, 0, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
			MPI_Recv(&number_of_docs_per_rank, 1, MPI_INT, 0, 1, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
		}


	}

	// Barrier for every rank to reach this place in sync
	MPI_Barrier(MPI_COMM_WORLD);

	
	// Logic: Loop through each document and gather TFICF variables for each word after each rank caluclates the tficf and unique_words array struct for its set of documents
	// Each worker node/ rank does the following using the TFICF and unique_word_struct for its set of documents
	if(rank != 0)
	{

		for(i= 0; i < number_of_docs_per_rank; i++){
			sprintf(document, "doc%d", i + start_i_for_rank_initial);
			sprintf(filename,"input/%s",document);
			FILE* fp = fopen(filename, "r");
			if(fp == NULL){
				printf("Error Opening File: %s\n", filename);
				exit(0);
			}
			
			// Get the document size
			docSize = 0;
			while((fscanf(fp,"%s",word))!= EOF)
				docSize++;
			
			// For each word in the document
			fseek(fp, 0, SEEK_SET);
			while((fscanf(fp,"%s",word))!= EOF){
				contains = 0;
				
				// If TFICF array already contains the word@document, just increment wordCount and break
				for(j=0; j<TF_idx; j++) {
					if(!strcmp(TFICF[j].word, word) && !strcmp(TFICF[j].document, document)){
						contains = 1;
						TFICF[j].wordCount++;
						break;
					}
				}
				
				//If TFICF array does not contain it, make a new one with wordCount=1
				if(!contains) {
					strcpy(TFICF[TF_idx].word, word);
					strcpy(TFICF[TF_idx].document, document);
					TFICF[TF_idx].wordCount = 1;
					TFICF[TF_idx].docSize = docSize;
					TFICF[TF_idx].numDocs = numDocs;
					TF_idx++;
				}
				
				contains = 0;
				// If unique_words array already contains the word, just increment numDocsWithWord
				for(j=0; j<uw_idx; j++) {
					if(!strcmp(unique_words[j].word, word)){
						contains = 1;
						if(unique_words[j].currDoc != i) {
							unique_words[j].numDocsWithWord++;
							unique_words[j].currDoc = i;
						}
						break;
					}
				}
				
				// If unique_words array does not contain it, make a new one with numDocsWithWord=1 
				if(!contains) {
					strcpy(unique_words[uw_idx].word, word);
					unique_words[uw_idx].numDocsWithWord = 1;
					unique_words[uw_idx].currDoc = i;
					uw_idx++;
				}
			}
			fclose(fp);
		}
	}

	// Before computing the TF and ICF values, message passing of the structures must take place to get the final numDocsWithWord for each word
	// Logic: Gathering array of structs from each rank and placing it in a large array of structs in the root node that can hold all these values and 
	// each word's numDocsWithWord is updated to the final value by adding its occurance in all docs

	// Setting up the struct object that needs to be gathered by the root from every rank
	int count = 3;
	MPI_Datatype array_of_types[count];
	array_of_types[0] = MPI_CHAR;
	array_of_types[1] = MPI_INT;
	array_of_types[2] = MPI_INT;
	int array_of_blocklengths[count];
	array_of_blocklengths[0] = 32;
	array_of_blocklengths[1] = 1;
	array_of_blocklengths[2] = 1;
	MPI_Aint array_of_displacements[count];
	MPI_Aint address1, address2, address3, address4;
	// For word element in struct
	MPI_Get_address(&w1, &address1);
	MPI_Get_address(&w1.word, &address2);
	array_of_displacements[0] = address2 - address1;
	// For numDocsWithWord element in struct
	MPI_Get_address(&w1.numDocsWithWord, &address3);
	array_of_displacements[1] = address3 - address1;
	// For currDoc element in struct- did not work without this element even though it is not necessary
	MPI_Get_address(&w1.currDoc, &address4);
	array_of_displacements[2] = address4 - address1;

	// Setting the name of the struct i.e type of the struct that we are going to send and receieve
	MPI_Datatype custom_uw_type;

	// Creating the struct
	MPI_Type_create_struct(count, array_of_blocklengths, array_of_displacements, array_of_types, &custom_uw_type);
	MPI_Type_commit(&custom_uw_type);

	// Creating an array that hold the count of words in unique_word of each rank
	int uw_idx_count[numProcs - 1];
	// Creating an array that hold the count of words in TFICF structs of each rank
	int tf_idx_count[numProcs - 1];
	
	// Array of struct for each rank to hold the unique_word_at_rank0 which would be sent back from root after updating the numDocWithWord for each word
	u_w *unique_word_at_each_rank;
	unique_word_at_each_rank = (u_w*)malloc(sizeof(u_w) * MAX_WORDS_IN_CORPUS * numProcs);

	// Holds the total number of words or structs in the array at each rank and at the root node respectively
	int total_uw_idx_each_rank, total_uw_idx;
	
	// count array used in gather
	int count_array_for_gather1[numProcs];
	// Displacement array for gather
	int disp_array_for_gather1[numProcs];
	int disp_for_gather1 = 0;

	//DEBUGGING:printf("%d\t After declarations\n", rank);
	
	// Rank 0 must gather each unique_words struct from each rank
	
	// Creating the struct that is going to be storing all the unique_word struct that is coming from each rank
	u_w *unique_word_at_rank0;
	unique_word_at_rank0 = (u_w*)malloc(sizeof(u_w) * MAX_WORDS_IN_CORPUS * numProcs);

	// All the necessary request structs for non-blocking calls
	MPI_Request req_for_irecv1[numProcs - 1], req_for_isend1[numProcs - 1];
	MPI_Request req_for_irecv2, req_for_isend2[numProcs - 1];
	MPI_Request req_for_gather1;

	// Barrier to ensure that all ranks reach this place- avoiding segmentaion faults or races
	MPI_Barrier(MPI_COMM_WORLD);

	if(rank == 0)
	{
		// Receiving the count i.e number of structs in each rank's unique_word array (number of words each rank is handling) from each node so that it can be used in rank 0 in the loop
		for(int i = 0; i < numProcs - 1; i++)
		{
			MPI_Irecv(&uw_idx_count[i], 1, MPI_INT, i + 1, i + 1, MPI_COMM_WORLD, &req_for_irecv1[i]); 
			
		}
		MPI_Waitall(numProcs - 1, req_for_irecv1, MPI_STATUS_IGNORE);
	
	}
	else 
	{	// Sending the number of words each rank is handling
		MPI_Isend(&uw_idx, 1, MPI_INT, 0, rank, MPI_COMM_WORLD, &req_for_isend1[rank-1]);
	}	
	// To compute count array and displacement arrays to be used in non-blocking version of gather. Array of structs only needed from all worker ranks except root
	if(rank == 0)
	{	
		count_array_for_gather1[0] = 0;
		total_uw_idx = 0;
		for(int i = 0; i < numProcs - 1; i++)
		{
			total_uw_idx += uw_idx_count[i];
			count_array_for_gather1[i+1] = uw_idx_count[i];
		}

		// Calculating the displacement for gather1
		disp_array_for_gather1[0] = 0;
		for(int rank_index = 1; rank_index < numProcs; rank_index++)
		{
			disp_array_for_gather1[rank_index] = disp_for_gather1;
			disp_for_gather1 += count_array_for_gather1[rank_index];
		}
	}
	
	// Avoiding segmentation faults
	MPI_Barrier(MPI_COMM_WORLD);
	// Performing Gather here
	MPI_Igatherv(unique_words, uw_idx, custom_uw_type, unique_word_at_rank0, count_array_for_gather1, disp_array_for_gather1, custom_uw_type, 0, MPI_COMM_WORLD, &req_for_gather1);
	MPI_Wait(&req_for_gather1, MPI_STATUS_IGNORE);

	if(rank == 0)
	{
		// Once all the unique_word structs from each rank are gathered into one big struct. Must loop through the struct and get the words and its total numDocsWithWords
		for(int i = 0; i < total_uw_idx; i++)
		{
			if(unique_word_at_rank0[i].numDocsWithWord != 0)
			{
				for(int j = i + 1; j < total_uw_idx; j++)
				{
					if(!strcmp(unique_word_at_rank0[i].word, unique_word_at_rank0[j].word))
					{	// Updating each word's numDocsWithWord to the final value by adding the different occurances of the same word's numDocsWithWord
						unique_word_at_rank0[i].numDocsWithWord += unique_word_at_rank0[j].numDocsWithWord;
						unique_word_at_rank0[j].numDocsWithWord = 0;
					}
				}
			}
		}
	}

	// To avoid races and seg faults
	MPI_Barrier(MPI_COMM_WORLD);
	if(rank == 0)
	{
		
		// Now that unique_word_at_rank0 has the proper numDocsWithWord value that has accounted for all the documents handled by different ranks, the struct must be
		// sent back to each rank for their respective TFICF calculations
		for(int i = 1; i < numProcs; i++)
		{	
			MPI_Isend(&total_uw_idx, 1, MPI_INT, i, 2*i, MPI_COMM_WORLD, &req_for_isend2[i-1]);
			//MPI_Isend(&unique_word_at_rank0, total_uw_idx, custom_uw_type, i, i, MPI_COMM_WORLD, &req_for_isend3[i-1]);
		}

	}
	// Sending the uw_idx of each rank to rank 0
	else
	{
		// Each rank getting the unique_word_at_rank0 struct and the total number of index values in the struct
		MPI_Irecv(&total_uw_idx_each_rank, 1, MPI_INT, 0, 2*rank, MPI_COMM_WORLD, &req_for_irecv2);
		MPI_Wait(&req_for_irecv2, MPI_STATUS_IGNORE);
	}

	// To avoid races and seg faults and ensure all ranks reach this point before sending the updated array of structs with numDocsWithWord back to all ranks from root
	MPI_Barrier(MPI_COMM_WORLD);

	// Making root send the updated array of struct with updated numDocsWithWord for each word to each of the worker rank that will use it to compute the tficf values
	if(rank == 0)
	{	
		MPI_Request send_req3[total_uw_idx];
		for(int i = 1; i < numProcs; i++)
		{	
			for(int j = 0; j < total_uw_idx; j++)
			{
				MPI_Isend(&unique_word_at_rank0[j], 1, custom_uw_type, i, j, MPI_COMM_WORLD, &send_req3[j]);
			}
			
		}
	} // Receiving the array of structs at each rank except rank 0 which is the master rank
	else 
	{	MPI_Request recv_req3[total_uw_idx_each_rank];
		
		for(int i = 0; i < total_uw_idx_each_rank; i++ )
		{
			MPI_Irecv(&unique_word_at_each_rank[i], 1, custom_uw_type, 0, i, MPI_COMM_WORLD, &recv_req3[i]);
		}
		
		MPI_Waitall(total_uw_idx_each_rank, recv_req3, MPI_STATUS_IGNORE);	
	}

	// Print TF job similar to HW4/HW5 (For debugging purposes)
	printf("-------------TF Job-------------\n");
	for(j=0; j<TF_idx; j++)
		printf("%s@%s\t%d/%d\n", TFICF[j].word, TFICF[j].document, TFICF[j].wordCount, TFICF[j].docSize);
		
	if(rank != 0)
	{
		// Use unique_words array to populate TFICF objects with: numDocsWithWord
		for(i=0; i<TF_idx; i++) {
			for(j=0; j < total_uw_idx_each_rank; j++) {
				if(!strcmp(TFICF[i].word, unique_word_at_each_rank[j].word) && (unique_word_at_each_rank[j].numDocsWithWord !=0)) {
					TFICF[i].numDocsWithWord = unique_word_at_each_rank[j].numDocsWithWord;	
					break;
				}
			}
		}
		
		// Print ICF job similar to HW4/HW5 (For debugging purposes)
		printf("------------ICF Job-------------\n");
		for(j=0; j<TF_idx; j++)
			printf("%s@%s\t%d/%d\n", TFICF[j].word, TFICF[j].document, TFICF[j].numDocs, TFICF[j].numDocsWithWord);
		
		// Calculates TFICF value and puts: "document@word\TFICF" into strings array
		for(j=0; j<TF_idx; j++) {
			double TF = log10( 1.0 * TFICF[j].wordCount / TFICF[j].docSize + 1 );
			double ICF = log10(1.0 * (TFICF[j].numDocs + 1) / (TFICF[j].numDocsWithWord + 1) );
			double TFICF_value = TF * ICF;
			sprintf(strings[j], "%s@%s\t%.16f", TFICF[j].document, TFICF[j].word, TFICF_value);
		}
	}
	
	// Logic: Every worker rank will compute the tficf values of each word in their set of documents. These words and their tficf values are stored in the strings array at each rank
	// This strings array is gathered by the master node i.e rank 0 and sort it

	// Collecting the tf_idx values from each rank in rank 0
	if(rank == 0)
	{	
		MPI_Request req_for_irecv4[numProcs -1];
		// Receiving the count from each node so that it can be used in rank 0 in the loop
		for(int i = 0; i < numProcs - 1; i++)
		{
			MPI_Irecv(&tf_idx_count[i], 1, MPI_INT, i + 1, i + 1, MPI_COMM_WORLD, &req_for_irecv4[i]); 
			
		}
		MPI_Waitall(numProcs - 1, req_for_irecv4, MPI_STATUS_IGNORE);
	
	}
	else 
	{	
		MPI_Request req_for_isend4[numProcs - 1];
		MPI_Isend(&TF_idx, 1, MPI_INT, 0, rank, MPI_COMM_WORLD, &req_for_isend4[rank-1]);
	}

	// Using simple datatype constructor for replication of datatype in contiguous locations
	MPI_Datatype custom_word_document_string;
	MPI_Type_contiguous(MAX_STRING_LENGTH, MPI_CHAR, &custom_word_document_string);
	MPI_Type_commit(&custom_word_document_string);
	
	// Calculating the displacement and count of each rank's string that is to be received by the root rank
	int *strings_count_array, *disp_array;
	int disp = 0;
	strings_count_array = (int *)malloc(numProcs * sizeof(int));
	disp_array = (int *)malloc(numProcs * sizeof(int));
	MPI_Request req_for_gather[1];

	int total_tf_idx = 0;

	if(rank == 0)
	{
		// Getting the total number of words that Strings array will have the data for
		for(int i = 0; i < numProcs - 1; i++)
		{
			total_tf_idx += tf_idx_count[i];
		}
		

		// Since we don't need the 0th rank's values, setting it to 0
		strings_count_array[0] = 0;
		disp_array[0] = 0;

		for(int rank_index = 1; rank_index < numProcs; rank_index++)
		{
			strings_count_array[rank_index] = tf_idx_count[rank_index - 1];
			disp_array[rank_index] = disp;
			disp += strings_count_array[rank_index];
		}
	}

	// Gathering each strings array from each rank into the rank 0
	MPI_Igatherv(strings, TF_idx, custom_word_document_string, strings, strings_count_array, disp_array, custom_word_document_string, 0, MPI_COMM_WORLD, &req_for_gather[0]);
	MPI_Waitall(1, req_for_gather, MPI_STATUS_IGNORE);

	if(rank == 0)
	{
		// All Strings would be gathered into strings which is looped for total_tf_idx i.e all the words from all docs for qsort
		// Sort strings and print to file
		qsort(strings, total_tf_idx, sizeof(char)*MAX_STRING_LENGTH, myCompare);
		FILE* fp = fopen("output.txt", "w");
		if(fp == NULL){
			printf("Error Opening File: output.txt\n");
			exit(0);
		}
		for(i=0; i<total_tf_idx; i++)
			fprintf(fp, "%s\n", strings[i]);
		fclose(fp);	
	}

	// Freeing the custom datatypes that were created to send and receive the required structs
	MPI_Type_free(&custom_word_document_string);
	MPI_Type_free(&custom_uw_type);
	MPI_Finalize();
	return 0;
}
