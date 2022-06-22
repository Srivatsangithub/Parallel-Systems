// Single Author Info:
// srames22 Srivatsan Ramesh

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.*;
import scala.Tuple2;

import java.util.*;

/*
 * Main class of the TFICF Spark implementation.
 * Author: Tyler Stocksdale
 * Date:   10/31/2017
 */
public class TFICF {

	static boolean DEBUG = false;

    public static void main(String[] args) throws Exception {
        // Check for correct usage
        if (args.length != 1) {
            System.err.println("Usage: TFICF <input dir>");
            System.exit(1);
        }
		
		// Create a Java Spark Context
		SparkConf conf = new SparkConf().setAppName("TFICF");
		JavaSparkContext sc = new JavaSparkContext(conf);

		// Load our input data
		// Output is: ( filePath , fileContents ) for each file in inputPath
		String inputPath = args[0];
		JavaPairRDD<String,String> filesRDD = sc.wholeTextFiles(inputPath);
		
		// Get/set the number of documents (to be used in the ICF job)
		long numDocs = filesRDD.count();
		
		//Print filesRDD contents
		if (DEBUG) {
			List<Tuple2<String, String>> list = filesRDD.collect();
			System.out.println("------Contents of filesRDD------");
			for (Tuple2<String, String> tuple : list) {
				System.out.println("(" + tuple._1 + ") , (" + tuple._2.trim() + ")");
			}
			System.out.println("--------------------------------");
		}
		
		/* 
		 * Initial Job
		 * Creates initial JavaPairRDD from filesRDD
		 * Contains each word@document from the corpus and also attaches the document size for 
		 * later use
		 * 
		 * Input:  ( filePath , fileContents )
		 * Map:    ( (word@document) , docSize )
		 */
		JavaPairRDD<String,Integer> wordsRDD = filesRDD.flatMapToPair(
			new PairFlatMapFunction<Tuple2<String,String>,String,Integer>() {
				public Iterable<Tuple2<String,Integer>> call(Tuple2<String,String> x) {
					// Collect data attributes
					String[] filePath = x._1.split("/");
					String document = filePath[filePath.length-1];
					String fileContents = x._2;
					String[] words = fileContents.split("\\s+");
					int docSize = words.length;
					
					// Output to Arraylist
					ArrayList ret = new ArrayList();
					for(String word : words) {
						ret.add(new Tuple2(word.trim() + "@" + document, docSize));
					}
					return ret;
				}
			}
		);
		
		//Print wordsRDD contents
		if (DEBUG) {
			List<Tuple2<String, Integer>> list = wordsRDD.collect();
			System.out.println("------Contents of wordsRDD------");
			for (Tuple2<String, Integer> tuple : list) {
				System.out.println("(" + tuple._1 + ") , (" + tuple._2 + ")");
			}
			System.out.println("--------------------------------");
		}		
		
		/* 
		 * TF Job (Word Count Job + Document Size Job)
		 * Gathers all data needed for TF calculation from wordsRDD
		 *
		 * Input:  ( (word@document) , docSize )
		 * Map:    ( (word@document) , (1/docSize) )
		 * Reduce: ( (word@document) , (wordCount/docSize) )
		 */
		JavaPairRDD<String,String> tfRDD = wordsRDD.mapToPair(
			
			/************ YOUR CODE HERE ************/
			// Input is a tuple of String, Integer from the initial job and the output is going to be a string, string
			new PairFunction<Tuple2<String, Integer>, String, String>() {
				public Tuple2<String, String> call(Tuple2<String, Integer> x) { 
					// From my understanding the function takes i/p, o/p (number of o/ps) 
					// and the method to implement takes the o/p and call works on the i/p 
					// and produces the one given before that as the o/p 
					String word_at_document = x._1;
					String one_by_docSize = "1/".concat(x._2.toString());
					Tuple2<String, String> map_output = new Tuple2<>(word_at_document, one_by_docSize);
					return map_output;
				}
			}
		).reduceByKey(
			
			/************ YOUR CODE HERE ************/
			// word@document is the key and the count for it must be reduced to the total word count and then that must be returned
			new Function2<String, String, String>() {
				public String call(String s1, String s2) {
					String[] value_1_array = s1.split("/");
					int value_1 = Integer.parseInt(value_1_array[0]);
					String[] value_2_array = s2.split("/");
					int value_2 = Integer.parseInt(value_2_array[0]);
					int wordCount = value_1 + value_2;
					String wordCount_by_DocSize = String.valueOf(wordCount).concat("/").concat(value_1_array[1]);
					return wordCount_by_DocSize;
						
				}
			}	
			
		);
		
		//Print tfRDD contents
		if (DEBUG) {
			List<Tuple2<String, String>> list = tfRDD.collect();
			System.out.println("-------Contents of tfRDD--------");
			for (Tuple2<String, String> tuple : list) {
				System.out.println("(" + tuple._1 + ") , (" + tuple._2 + ")");
			}
			System.out.println("--------------------------------");
		}
		
		/*
		 * ICF Job
		 * Gathers all data needed for ICF calculation from tfRDD
		 *
		 * Input:  ( (word@document) , (wordCount/docSize) )
		 * Map:    ( word , (1/document) )
		 * Reduce: ( word , (numDocsWithWord/document1,document2...) )
		 * Map:    ( (word@document) , (numDocs/numDocsWithWord) )
		 */
		JavaPairRDD<String,String> icfRDD = tfRDD.mapToPair(
			
			/************ YOUR CODE HERE ************/
			// Need to get the first string from the input that is coming and process it to be the input for the ReduceByKey phase
			new PairFunction<Tuple2<String, String>, String, String>() {
				public Tuple2<String, String> call(Tuple2<String, String> x) {
					
					// Rearraging the input i.e x tuple's 0th element to send it as an input to the reduce function
					String word_at_document = x._1;
					String [] word_document_array = word_at_document.split("@");
					String word_to_send = word_document_array[0];
					String doc_to_send = "1/".concat(word_document_array[1]);
					Tuple2<String, String> map_tuple_to_send = new Tuple2<>(word_to_send, doc_to_send);
					return map_tuple_to_send;

				}
			}
			
		).reduceByKey(
			
			/************ YOUR CODE HERE ************/
			// Reducing by key i.e the word after converting the numerical part to integer and appending the document name to the string for all reductions
			new Function2<String, String, String>() {
				// output is going to be a string with the number of documents with the key/doc names
				public String call(String s1, String s2) {
					String[] value_1_array = s1.split("/");
					int value_1 = Integer.parseInt(value_1_array[0]);
					String[] value_2_array = s2.split("/");
					int value_2 = Integer.parseInt(value_2_array[0]);
					int docCount = value_1 + value_2;
					String docCount_by_docs = String.valueOf(docCount).concat("/").concat(value_1_array[1]).concat(",").concat(value_2_array[1]);
					return docCount_by_docs;
						
				}
			}
		// Now for each key which would have several document name eg.: ipsum is present in doc1 and doc2, mapping it to the corresponding document. Since each key
		// will have multiple values, using flatMap like functions for pairRDD	
		).flatMapToPair(
			
			/************ YOUR CODE HERE ************/
			new PairFlatMapFunction<Tuple2<String, String>, String, String>() {
				public Iterable<Tuple2<String, String>> call(Tuple2<String,String> x) {
					// Get the word i.e the key for this mapper
					String word = x._1;
					String [] numDocs_by_docName_array = x._2.split("/");
					String [] docNamesString = numDocs_by_docName_array[1].split(",");
					// Looping through the list of document names that has been extracted using the split method

					// Output to Arraylist
					ArrayList ret = new ArrayList();
					for(String doc : docNamesString) {
						ret.add(new Tuple2(word.trim().concat("@").concat(doc), String.valueOf(numDocs).concat("/").concat(numDocs_by_docName_array[0])));
					}
					return ret;

				}
			}
			
		);
		
		//Print icfRDD contents
		if (DEBUG) {
			List<Tuple2<String, String>> list = icfRDD.collect();
			System.out.println("-------Contents of icfRDD-------");
			for (Tuple2<String, String> tuple : list) {
				System.out.println("(" + tuple._1 + ") , (" + tuple._2 + ")");
			}
			System.out.println("--------------------------------");
		}
	
		/*
		 * TF * ICF Job
		 * Calculates final TFICF value from tfRDD and icfRDD
		 *
		 * Input:  ( (word@document) , (wordCount/docSize) )          [from tfRDD]
		 * Map:    ( (word@document) , TF )
		 * 
		 * Input:  ( (word@document) , (numDocs/numDocsWithWord) )    [from icfRDD]
		 * Map:    ( (word@document) , ICF )
		 * 
		 * Union:  ( (word@document) , TF )  U  ( (word@document) , ICF )
		 * Reduce: ( (word@document) , TFICF )
		 * Map:    ( (document@word) , TFICF )
		 *
		 * where TF    = log( wordCount/docSize + 1 )
		 * where ICF   = log( (Total numDocs in the corpus + 1) / (numDocsWithWord in the corpus + 1) )
		 * where TFICF = TF * ICF
		 */
		JavaPairRDD<String,Double> tfFinalRDD = tfRDD.mapToPair(
			new PairFunction<Tuple2<String,String>,String,Double>() {
				public Tuple2<String,Double> call(Tuple2<String,String> x) {
					double wordCount = Double.parseDouble(x._2.split("/")[0]);
					double docSize = Double.parseDouble(x._2.split("/")[1]);
					double TF = Math.log10(wordCount/docSize+1);
					return new Tuple2(x._1, TF);
				}
			}
		);
		
		JavaPairRDD<String,Double> idfFinalRDD = icfRDD.mapToPair(
			
			/************ YOUR CODE HERE ************/
			// To calculate icf part we get the numDocs and numDocsWithWord and apply the formula
			new PairFunction<Tuple2<String,String>,String,Double>() {
				public Tuple2<String,Double> call(Tuple2<String,String> x) {
					double numOfDocs = Double.parseDouble(x._2.split("/")[0]);
					double numDocsWithWord = Double.parseDouble(x._2.split("/")[1]);
					double ICF = Math.log10(((numOfDocs + 1) /(numDocsWithWord + 1)));
					return new Tuple2(x._1, ICF);
				}

			}
			
		);
		
		JavaPairRDD<String,Double> tficfRDD = tfFinalRDD.union(idfFinalRDD).reduceByKey(
			
			/************ YOUR CODE HERE ************/
			// To reduce i.e to multiply the tf and icf values for each word@document that is the key
			new Function2<Double, Double, Double>() {
				public Double call(Double tf, Double icf) {
					// Multiplying tf and icf for each key
					Double tficf = tf * icf;
					return tficf;

				}

			}
			
		).mapToPair(
			
			/************ YOUR CODE HERE ************/
			// To map each word@document to document@word
			new PairFunction<Tuple2<String, Double>, String, Double>() {
				public Tuple2<String, Double> call(Tuple2<String, Double> x) {
					String[] word_at_document_array = x._1.split("@");
					String document_at_word = word_at_document_array[1].concat("@").concat(word_at_document_array[0]);
					Tuple2<String, Double> final_tuple = new Tuple2<>(document_at_word, x._2);
					return final_tuple;

				}
			}


			
		);
		
		//Print tficfRDD contents in sorted order
		Map<String, Double> sortedMap = new TreeMap<>();
		List<Tuple2<String, Double>> list = tficfRDD.collect();
		for (Tuple2<String, Double> tuple : list) {
			sortedMap.put(tuple._1, tuple._2);
		}
		if(DEBUG) System.out.println("-------Contents of tficfRDD-------");
		for (String key : sortedMap.keySet()) {
			System.out.println(key + "\t" + sortedMap.get(key));
		}
		if(DEBUG) System.out.println("--------------------------------");	 
	}	
}