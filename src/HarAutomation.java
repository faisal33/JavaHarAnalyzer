
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import edu.umass.cs.benchlab.har.*;
import edu.umass.cs.benchlab.har.tools.*;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.JsonParseException;

public class HarAutomation {
	
	static Boolean withWrapper = false;
	static Boolean noWrapper = false;
	static List<Double> index = new ArrayList<Double>();
	static List<Double> noIndex = new ArrayList<Double>();

	public static void main(String[] args) {

		String fileName = null; 
		String filePath = null;
		double timeTaken = 0.0;
		String fileType = null;
		double indexAverage = 0.0;
		double noIndexAverage = 0.0;
		File folder = new File(args[0]);
		String noWrapperUrl = args[1];
		String wrapperUrl = null;
		if(args[2] != null){
			wrapperUrl = args[2];
		}

		File[] listOfFiles = folder.listFiles((dir, name) -> !name.equals(".DS_Store"));
		
		for (int i = 0; i < listOfFiles.length; i++) {
		      if (listOfFiles[i].isFile()) {
		    	  fileName = listOfFiles[i].getName();
		    	  timeTaken = getTime(folder.getName(),fileName,noWrapperUrl,wrapperUrl);	  
		      }
		}
		calculateAndPrintAverage();
	}

	/**
	 * This function calculates and prints the averages for Page Loads with and without wrapper
	 * @param indexAverage
	 * @param noIndexAverage
	 */
	private static void calculateAndPrintAverage() {
		
		Double indexSd = getStdDevIndex();
		Double noIndexSd = getStdDevNoIndex();
		Double indexAverage = getMeanIndex(indexSd);
		Double noIndexAverage = getMeanNoIndex(noIndexSd);

		System.out.printf("%n");
		if(index.size() > 0)
		System.out.println(" Average Time with Index Wrapper = " + Math.round (indexAverage * 10000.0) / 10000.0 + " s");
		System.out.printf("%n");
		if(noIndex.size() > 0)
		System.out.println(" Average Time without Index Wrapper = " + Math.round (noIndexAverage * 10000.0) / 10000.0 + " s");
		System.out.printf("%n");
	}
	
/**
 * This function go through the HAR files and get the times
 * @param folder
 * @param fileName
 * @return
 */
	private static double getTime(String folder,String fileName, String noWrapperUrl, String wrapperUrl) {
		Double initialLoadTime = null;
		Double adLoadTime = null;
		Double loadTime = 0.0;
		try
		    {
		    	 List<String> tobeDeleted = new ArrayList<String>();
		         List<String> newLines = new ArrayList<>();
		  	     int count = 0;
		  	     Path harFilePath = Paths.get(folder, fileName);
		  	     
		    	File f = new File(harFilePath.toString());
		        HarFileReader r = new HarFileReader();
		        HarLog log = r.readHarFile(f);
		        HarEntries entries = log.getEntries();
		        List<HarEntry> hentry = entries.getEntries();
		        double initialTime = 0.0;
		        for (HarEntry entry : hentry)
		        {
		            if(entry.getRequest().getUrl().equals(noWrapperUrl) || entry.getRequest().getUrl().equals(wrapperUrl) ){
		            	String einitialTime = entry.toString();
		            	if(einitialTime.contains("startedDateTime")){
		            		initialLoadTime = getTimeinMilli(einitialTime);		
		            		initialTime = entry.getTime()/1000.00;
		            	}
		            }
		            if(entry.getRequest().getUrl().contains("ads?gdfp")){
		            	String einitialTime = entry.toString();
		            	if(einitialTime.contains("startedDateTime")){
		            		adLoadTime = getTimeinMilli(einitialTime);	
		            		break;
		            	}
		            }
		        }
		        if(adLoadTime == null){
		        	return 0.0;
		        }
		        loadTime = timeDifference(initialLoadTime, adLoadTime)/1000.00; 
		        if(fileName.contains("nowrapper")){
		        	if(noWrapper == false){
		        		System.out.printf("%n");
		        	System.out.println("---------------------- Without Wrapper -------------------");
		        	System.out.printf("%n");
		        	noWrapper = true;
		        	}
		        }
		        else{
		        	if(withWrapper == false){
		        		System.out.printf("%n");
		        		System.out.println("---------------------- Index Wrapper -------------------");
		        		System.out.printf("%n");
			        	withWrapper = true;
			        	}
		        	
		        }
		        System.out.println("Initial Page load Time " + initialTime + " s" + "  |  " + "Time before first ad loads " + loadTime + " s" + "  |  " + " Difference " + (loadTime - initialTime) + " s");
		        if(fileName.contains("nowrapper")){
		        	noIndex.add((loadTime - initialTime));
		        }
		        else{
		        	index.add((loadTime - initialTime));
		        }
		    }
		    catch (JsonParseException e)
		    {
		      e.printStackTrace();
		    } catch (IOException e) {
				e.printStackTrace();
			}
		return loadTime;
		
	}

/**
 * Finds the difference between the ad loads
 * @param initialLoadTime
 * @param adLoadTime
 * @return
 */
	private static double timeDifference(double initialLoadTime, double adLoadTime) {
		return adLoadTime - initialLoadTime;
	}
	
/**
 * Convert times from ISO8601 into milliseconds
 * @param einitialTime
 * @return
 */
	private static double getTimeinMilli(String einitialTime) {
		int state = einitialTime.indexOf("startedDateTime");
		String extractingTime = einitialTime.substring(state, state+50);
		String subString = extractingTime.substring(extractingTime.indexOf("2")-2, extractingTime.length()-1);
		int lastindex = subString.lastIndexOf("-");
		subString = subString.substring(2,lastindex);
		long millisFromEpoch = Instant.parse(subString+"Z").toEpochMilli();
		return millisFromEpoch;
	}
	
	/**
	 * @param indexAverage
	 * @return
	 */
	private static double getMeanIndex() {
		double indexAverage = 0.0;
		for (int i = 0; i< index.size(); i++)
		{
			indexAverage = indexAverage  + index.get(i);
		}
		indexAverage = indexAverage/index.size();
		return indexAverage;
	}
	
	/**
	 * @param indexAverage
	 * @return
	 */
	private static double getMeanNoIndex() {
		double indexAverage = 0.0;
		for (int i = 0; i< noIndex.size(); i++)
		{
			indexAverage = indexAverage  + noIndex.get(i);
		}
		indexAverage = indexAverage/noIndex.size();
		return indexAverage;
	}
	
	/**
	 * Remove Outliers
	 * @param standardDev
	 * @return
	 */
	private static double getMeanIndex(double standardDev) {
		double indexAverage = 0.0;
		double size = 0;
		for (int i = 0; i< index.size(); i++)
		{
			if(index.get(i) > standardDev){
				size+=1;
			}
			else{
				indexAverage = indexAverage  + index.get(i);
			}
		}
		indexAverage = indexAverage/index.size();
		return indexAverage;
	}
	
 /**
  * Overload mean function to remove outliers
  * @param standardDev
  * @return
  */
	private static double getMeanNoIndex(double standardDev) {
		double indexAverage = 0.0;
		double size = 0;
		for (int i = 0; i< noIndex.size(); i++)
		{
			if(noIndex.get(i) > standardDev){
				size+=1;
			}
			else{
				indexAverage = indexAverage  + noIndex.get(i);
			}

		}
		indexAverage = indexAverage/(noIndex.size() - size);
		return indexAverage;
	}
	
	/**
	 * Calculate Variance
	 * @return
	 */

	private static double getVarianceIndex()
    {
        double mean = getMeanIndex();
        double temp = 0;
        for (int i = 0; i< index.size(); i++){
        	temp += (index.get(i)-mean)*(index.get(i)-mean);
        }   
        return temp/index.size();
    }
	
    /**
     * 
     * @return
     */
	private static double getVarianceNoIndex()
    {
        double mean = getMeanNoIndex();
        double temp = 0;
        for (int i = 0; i< index.size(); i++){
        	temp += (noIndex.get(i)-mean)*(noIndex.get(i)-mean);
        }
        return temp/noIndex.size();
    }
	/**
	 * Calculate Standard Deviation of times with IndexWrapper
	 * @return
	 */
	private static double getStdDevIndex()
    {
        return Math.sqrt(getVarianceIndex());
    }
	/**
	 * Calculate Standard Deviation of times without IndexWrapper
	 * @return
	 */
	private static double getStdDevNoIndex()
    {
        return Math.sqrt(getVarianceNoIndex());
    }

}

