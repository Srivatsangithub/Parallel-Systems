// Single Author Info:
// srames22 Srivatsan Ramesh

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

//import org.apache.hadoop.mapred;

import java.io.IOException;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Main class of the TFICF MapReduce implementation.
 * Author: Tyler Stocksdale
 * Date:   10/18/2017
 */
public class TFICF {

    public static void main(String[] args) throws Exception {
        // Check for correct usage
        if (args.length != 2) {
            System.err.println("Usage: TFICF <input corpus0 dir> <input corpus1 dir>");
            System.exit(1);
        }
		
		// return value of run func
		int ret = 0;
		
		// Create configuration
		Configuration conf0 = new Configuration();
		Configuration conf1 = new Configuration();
		
		// Input and output paths for each job
		Path inputPath0 = new Path(args[0]);
		Path inputPath1 = new Path(args[1]);
        try{
            ret = run(conf0, inputPath0, 0);
        }catch(Exception e){
            e.printStackTrace();
        }
        if(ret == 0){
        	try{
            	run(conf1, inputPath1, 1);
        	}catch(Exception e){
            	e.printStackTrace();
        	}        	
        }
     
     	System.exit(ret);
    }
		
	public static int run(Configuration conf, Path path, int index) throws Exception{
		// Input and output paths for each job

		Path wcInputPath = path;
		Path wcOutputPath = new Path("output" +index + "/WordCount");
		Path dsInputPath = wcOutputPath;
		Path dsOutputPath = new Path("output" + index + "/DocSize");
		Path tficfInputPath = dsOutputPath;
		Path tficfOutputPath = new Path("output" + index + "/TFICF");
		
		// Get/set the number of documents (to be used in the TFICF MapReduce job)
        FileSystem fs = path.getFileSystem(conf);
        FileStatus[] stat = fs.listStatus(path);
		String numDocs = String.valueOf(stat.length);
		conf.set("numDocs", numDocs);
		
		// Delete output paths if they exist
		FileSystem hdfs = FileSystem.get(conf);
		if (hdfs.exists(wcOutputPath))
			hdfs.delete(wcOutputPath, true);
		if (hdfs.exists(dsOutputPath))
			hdfs.delete(dsOutputPath, true);
		if (hdfs.exists(tficfOutputPath))
			hdfs.delete(tficfOutputPath, true);
		
		// Create and execute Word Count job
		
			/************ YOUR CODE HERE ************/
			Job job_wc = Job.getInstance(conf, "word count");
			//Job job_wc = new Job(conf, "word count");
			job_wc.setJarByClass(TFICF.class);
			job_wc.setMapperClass(WCMapper.class);
			job_wc.setCombinerClass(WCReducer.class);
			job_wc.setReducerClass(WCReducer.class);
			job_wc.setOutputKeyClass(Text.class);
			job_wc.setOutputValueClass(IntWritable.class);
			FileInputFormat.addInputPath(job_wc, wcInputPath);
			FileOutputFormat.setOutputPath(job_wc, wcOutputPath);

			//JobClient.runJob(job_wc);
			job_wc.waitForCompletion(true);
			
		// Create and execute Document Size job
		
			/************ YOUR CODE HERE ************/
			Job job_ds = Job.getInstance(conf, "document size");
			//Job job_ds = new Job(conf, "document size");
			//job_ds.setJarByClass(TFICF.class);
			job_ds.setMapperClass(DSMapper.class);
			//job_ds.setCombinerClass(DSReducer.class);
			job_ds.setReducerClass(DSReducer.class);
			job_ds.setOutputKeyClass(Text.class);
			job_ds.setOutputValueClass(Text.class);
			FileInputFormat.addInputPath(job_ds, dsInputPath);
			FileOutputFormat.setOutputPath(job_ds, dsOutputPath);
			job_ds.waitForCompletion(true);
			//JobClient.runJob(job_ds);
		
		//Create and execute TFICF job
		
			/************ YOUR CODE HERE ************/
			Job job_tficf = Job.getInstance(conf, "TFICF");
			//job_ds.setJarByClass(TFICF.class);
			job_tficf.setMapperClass(TFICFMapper.class);
			job_tficf.setReducerClass(TFICFReducer.class);
			job_tficf.setOutputKeyClass(Text.class);
			job_tficf.setOutputValueClass(Text.class);
			FileInputFormat.addInputPath(job_tficf, tficfInputPath);
			FileOutputFormat.setOutputPath(job_tficf, tficfOutputPath);

		//Return final job code , e.g. retrun tficfJob.waitForCompletion(true) ? 0 : 1
			/************ YOUR CODE HERE ************/
			return (job_tficf.waitForCompletion(true) ? 0 : 1);	
    }
	
	/*
	 * Creates a (key,value) pair for every word in the document 
	 *
	 * Input:  ( byte offset , contents of one line )
	 * Output: ( (word@document) , 1 )
	 *
	 * word = an individual word in the document
	 * document = the filename of the document
	 */
	public static class WCMapper extends Mapper<Object, Text, Text, IntWritable> {
		
		/************ YOUR CODE HERE ************/
		// Initial value/count for all words will be one/1 
		private final static IntWritable one = new IntWritable(1);
		// word will contain the token/each word after StringTokenizer()
		private Text word = new Text();

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

			// To get the filename to append to key for further jobs
			String docName = ((FileSplit) context.getInputSplit()).getPath().getName();
			
			// Gets your lines 
			String line = value.toString();

			// // To ensure special characters are eliminated and only alphanumeric characters are chosen as a word
			Pattern p = Pattern.compile("[-a-zA-Z0-9À-ÿ_.?\'()]+");
			Matcher m = p.matcher(line);

			// StringBuilder for appending and getting the key
			// StringBuilder keyBuilder = new StringBuilder();

			// while there is a match, find groups and check if starting character of groups is legal, if so continue with building key
			while(m.find()){
				String matchedKey = m.group().toLowerCase();
				if(matchedKey.contains("'")){
                	matchedKey = matchedKey.replace("'", "");
            	}
				if(matchedKey.contains("?")){
                	matchedKey = matchedKey.replace("?", "");
            	}
				if(matchedKey.contains(".")){
                	matchedKey = matchedKey.replace(".", "");
            	}
				if(matchedKey.contains("(")){
                	matchedKey = matchedKey.replace("(", "");
            	}
				if(matchedKey.contains(")")){
                	matchedKey = matchedKey.replace(")", "");
            	}
				if(matchedKey.length() == 0){
                	continue;
            	}
				else if(!Character.isLetter(matchedKey.charAt(0)) || Character.isDigit(matchedKey.charAt(0)) || matchedKey.contains("_") || matchedKey.contains("@")) {
					continue;
				}
				// keyBuilder.append(matchedKey);
				// keyBuilder.append("@");
				// keyBuilder.append(docName);
				matchedKey = matchedKey.concat("@").concat(docName);
				word.set(matchedKey);
				context.write(word, one);
				//System.out.println("INSIDE WCMAPPER:" + " " + word.toString() + " " + one.toString());
			}

		// 	// Tokenizes the line into separate words
		// 	StringTokenizer itr = new StringTokenizer(line);
		// 	while(itr.hasMoreTokens()){
		// 		word.set(itr.nextToken());
		// 		keyBuilder.append(word);
		// 		keyBuilder.append("@");
		// 		keyBuilder.append(docName);
		// 		word.set(keyBuilder.toString());
		// 		context.write(word, one);
		// 	}
		}
		
    }

    /*
	 * For each identical key (word@document), reduces the values (1) into a sum (wordCount)
	 *
	 * Input:  ( (word@document) , 1 )
	 * Output: ( (word@document) , wordCount )
	 *
	 * wordCount = number of times word appears in document
	 */
	public static class WCReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		
		/************ YOUR CODE HERE ************/
		// To find the sum and set it to wordCount
		private IntWritable wordCount = new IntWritable();
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum +=val.get();
			}
			wordCount.set(sum);
			context.write(key, wordCount);
		
    	}
	}
	
	/*
	 * Rearranges the (key,value) pairs to have only the document as the key
	 *
	 * Input:  ( (word@document) , wordCount )
	 * Output: ( document , (word=wordCount) )
	 */
	public static class DSMapper extends Mapper<Object, Text, Text, Text> {
		
		/************ YOUR CODE HERE ************/
		private Text document_name = new Text();
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
		// After getting the key, value pair for each legal word with document name and its count, it is passed to input_from_prev_job
		String input_from_prev_job = value.toString();
		String[] word_doc_count_arr = input_from_prev_job.split("\t");	// To separate the key, value from previous job
		String[] word_doc_arr = word_doc_count_arr[0].split("@");		// Getting the document name 
		document_name.set(word_doc_arr[1]);								
		Text word_equal_wordCount = new Text(word_doc_arr[0] + "=" + word_doc_count_arr[1]);
		context.write(document_name, word_equal_wordCount);				// Writing the required key, value pair
		//System.out.println("INSIDE DSMAPPER:" + " " + document_name.toString() + " " + word_equal_wordCount.toString());
    	}
	}

    /*
	 * For each identical key (document), reduces the values (word=wordCount) into a sum (docSize) 
	 *
	 * Input:  ( document , (word=wordCount) )
	 * Output: ( (word@document) , (wordCount/docSize) )
	 *
	 * docSize = total number of words in the document
	 */
	public static class DSReducer extends Reducer<Text, Text, Text, Text> {
		
		/************ YOUR CODE HERE ************/
		private Text word_key = new Text();
		private Text word_cnt = new Text();

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			// Initializing a Dictionary
        	//Dictionary word_count_dict = new Hashtable();
			// Initializing a map instead
			Map<String, Integer> word_count_map = new HashMap<String, Integer>();
			int sum_per_document = 0;
			String docu_key_name = key.toString();	// Getting the document name which is the key from the previous job
			for (Text val : values){		// Need to save the incoming words and their counts as key value pairs so that we could maybe access it later for context.write maybe
				String[] temp_arr = val.toString().split("=");	// Each iterable element in values is a Text so convert it and split
				sum_per_document += Integer.parseInt(temp_arr[1]);	// To calculate the total sum of words(all occurences) in the document 
				//word_count_dict.put(temp_arr[0], Integer.parseInt(temp_arr[1]));
				word_count_map.put(temp_arr[0], Integer.parseInt(temp_arr[1]));		// Adding the key in dict as the word we got and the count of the word as the value
			}
			// Enumeration<String> e = word_count_dict.keys();
			// while(e.hasMoreElements()){
			// 	String k = e.nextElement();
			// 	String word_key = k.concat("@").concat(docu_key_name);
			// 	String word_cnt = word_count_dict.get(k).toString();
			// 	word_cnt = word_cnt.concat("/").concat(Integer.toString(sum_per_document));
			// 	// word_key.set(k);
			// 	// word_cnt.set(word_count_dict.get(k));
			// 	context.write(new Text(word_key), new Text(word_cnt));
			// }

			// For each word in the map which has count values we concat and write it with the corresponding document name and total sum of words
			for (String k : word_count_map.keySet()) {
				String word_key = k.concat("@").concat(docu_key_name);
				String word_cnt = word_count_map.get(k).toString();
				word_cnt = word_cnt.concat("/").concat(Integer.toString(sum_per_document));
            	context.write(new Text(word_key), new Text(word_cnt));
        }

		// 	int sumOfWordsInDocument = 0;
        // 	Map<String, Integer> tempCounter = new HashMap<String, Integer>();
        // 	for (Text val : values) {
        //     	String[] wordCounter = val.toString().split("=");
        //     	tempCounter.put(wordCounter[0], Integer.valueOf(wordCounter[1]));
        //     	sumOfWordsInDocument += Integer.parseInt(val.toString().split("=")[1]);
        // 	}
        // 	for (String wordKey : tempCounter.keySet()) {
        //     	context.write(new Text(wordKey + "@" + key.toString()), new Text(tempCounter.get(wordKey) + "/" + sumOfWordsInDocument));
        // }
		}		
    
	}
	
	/*
	 * Rearranges the (key,value) pairs to have only the word as the key
	 * 
	 * Input:  ( (word@document) , (wordCount/docSize) )
	 * Output: ( word , (document=wordCount/docSize) )
	 */
	public static class TFICFMapper extends Mapper<Object, Text, Text, Text> {

		/************ YOUR CODE HERE ************/
		private Text key_word = new Text();
		private Text map_val = new Text();
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			// Splitting the recieving input from the previous job and splitting the string based on what we need to send to the reducer
			String[] prev_mapper_ip = value.toString().split("\t");
			String[] word_at_doc = prev_mapper_ip[0].split("@");
			
			key_word.set(word_at_doc[0]);
			String mapper_val = word_at_doc[1].concat("=").concat(prev_mapper_ip[1]);
			map_val.set(mapper_val);
			
			context.write(key_word, map_val);
			//System.out.println("Inside TFICF: " + key_word.toString() + " " +  map_val.toString());

		}

		
    }

    /*
	 * For each identical key (word), reduces the values (document=wordCount/docSize) into a 
	 * the final TFICF value (TFICF). Along the way, calculates the total number of documents and 
	 * the number of documents that contain the word.
	 * 
	 * Input:  ( word , (document=wordCount/docSize) )
	 * Output: ( (document@word) , TFICF )
	 *
	 * numDocs = total number of documents
	 * numDocsWithWord = number of documents containing word
	 * TFICF = ln(wordCount/docSize + 1) * ln(numDocs/numDocsWithWord +1)
	 *
	 * Note: The output (key,value) pairs are sorted using TreeMap ONLY for grading purposes. For
	 *       extremely large datasets, having a for loop iterate through all the (key,value) pairs 
	 *       is highly inefficient!
	 */
	public static class TFICFReducer extends Reducer<Text, Text, Text, Text> {
		
		private static int numDocs;
		private Map<Text, Text> tficfMap = new HashMap<>();
		
		// gets the numDocs value and stores it
		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			numDocs = Integer.parseInt(conf.get("numDocs"));
		}
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			
			/************ YOUR CODE HERE ************/
			// 1: to find the numDocsWithWord
			int numDocsWithWord = 0;
			Map<String, String> map_for_tficf = new HashMap<String, String>();
			
			for (Text val : values){
				
				// Since every val for that key is going to be a distinct document
				numDocsWithWord += 1;
				String[] doc_tf_split = val.toString().split("=");
				map_for_tficf.put(doc_tf_split[0], doc_tf_split[1]);
				}	
			for (String docs : map_for_tficf.keySet()){	
				
				// 2. to compute the tf part- splitting the wordCount/docSize from input to actually compute the value
				String[] tf_split = map_for_tficf.get(docs).split("/");

				// term frequency
				double tf_part = Math.log10((Double.parseDouble(tf_split[0])/ Double.parseDouble(tf_split[1])) + 1);
				
				// 3. to compute icf
				double icf_part = Math.log10(((numDocs + 1) /(numDocsWithWord + 1)));

				// 4. tficf part
				double tficf_final = tf_part * icf_part;
				String document_at_word = docs.concat("@").concat(key.toString());
				Text doc = new Text();
				Text tficf_val = new Text();
				doc.set(document_at_word);
				tficf_val.set(Double.toString(tficf_final));
				
				//Put the output (key,value) pair into the tficfMap instead of doing a context.write
				tficfMap.put(doc, tficf_val);
				}	

			}
		
		// sorts the output (key,value) pairs that are contained in the tficfMap
		protected void cleanup(Context context) throws IOException, InterruptedException {
            Map<Text, Text> sortedMap = new TreeMap<Text, Text>(tficfMap);
			for (Text key : sortedMap.keySet()) {
                context.write(key, sortedMap.get(key));
            }
        }
		
    }
}
