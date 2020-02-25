package edu.ncsu.las.collector.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import org.apache.tika.Tika;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import edu.ncsu.las.model.collector.type.MimeType;
enum TableType{
	Row, Column, Layout;
}
class TableStatistics{
	 double _stdDev;
	 int _totalTd;
	 int _totalTagsInTd;
	 int _mean;
	 TableType _type;
	 
	 @Override
	 public String toString(){
		 //return "stdDev:"+_stdDev+" totalTd:"+_totalTd+" totalTagsInTd:"+_totalTagsInTd+" mean:"+_mean+" type:"+_type;
		 return _stdDev+", "+_type;
	 }
}
public class TableAnalyzer {

	private static TableStatistics _ts;
	
	public static void main(String[] args) throws IOException {

		//Document doc = Jsoup.connect("http://www.doit.wisc.edu/outside-cms/accessibility/online-course/standards/tables.htm").get();
		
		File dir = new File("/Users/users/Desktop/Leonia/LAS/collector_drone_files");
		FileWriter fw = new FileWriter("/Users/users/Desktop/Leonia/LAS/DroneTables.csv");
		for(File f:dir.listFiles()){
			if(new Tika().detect(f)!=MimeType.TEXT_HTML)
				continue;
			Document doc = Jsoup.parse(f, "UTF-8");
			String filename = f.getName();
			int tableNumber=1;
			Elements tables = doc.select("table");
			_ts = getTableStatistics(doc);
			if(_ts==null)
				continue;
			
			for(Element table:tables){
				//String tablename = filename+"_"+tableNumber++;
				TableStatistics localts = getTableType(table); //getTableType gets the statistics as well as type for the table.
				if(localts!=null){
					// Code to write to csv here.
					System.out.println(localts);
					fw.append(filename);
					fw.append(',');
					fw.append(""+tableNumber);
					fw.append(',');
					fw.append(localts.toString());
					//fw.append(',');
					//fw.append("\""+table.html()+"\"");
					fw.append('\n');
					tableNumber++;
				}
			}
		}
		fw.flush();
		fw.close();
	}
	/**
	 * Finds the standard deviation of the html document
	 * and saves it in a class variable. 
	 */
	private static TableStatistics getTableStatistics(Element doc){
		if(doc==null)
			return null;
		TableStatistics ts = new TableStatistics();
		Elements td = doc.select("td");
		ts._totalTd=td.size();
		if(td.size()==0)
			return null;
		for(Element t:td){
			ts._totalTagsInTd+=getNumberOfChildTags(t);
		}
		ts._mean = ts._totalTagsInTd/ts._totalTd;
		//System.out.println("HTML tags in td in total="+ts._totalTagsInTd+", #td="+ts._totalTd + ", mean="+ts._mean);
		
		//For standard deviation:
		int sum=0;
		for(Element t:td){
			sum+=Math.pow(getNumberOfChildTags(t)-ts._mean,2);
		}
		ts._stdDev = Math.sqrt(sum/ts._totalTd);
		//System.out.println("Standard deviation="+ts._stdDev);
		
		return ts;
	}

	/**
	 * Returns total number of html tags in a <td>.
	 *
	 */
	private static int getNumberOfChildTags(Element t) {
		Elements tags = t.children();
		int size=tags.size();
		if(size!=0){
			for(Element tg:tags){
				size+=getNumberOfChildTags(tg);
			}
		}
		
		return size;
	}
	
	/**
	 * Returns whether the table is row oriented or column oriented or just a layout table.
	 * 
	 */
	private static TableStatistics getTableType(Element table){
		TableStatistics localts = null;
		if(table!=null){
			Elements tables = table.select("table");
			localts = getTableStatistics(tables.first());
			
			if(tables.size()>1) //Layout tables may have child tables.
				localts._type = TableType.Layout;
			else{
				if(localts._stdDev<=_ts._stdDev){
					Elements tr_tags = table.select("tr");
					
					for(Element tr:tr_tags){
						//check if first <tr> has <th> tags
						Elements first_tr_ths = tr.select("th[colspan]");
						if(first_tr_ths.size()>0){
							continue; //Ignore table caption in <th> tag. They generally have a colspan
						}
						else{
							Elements ths = tr.select("th");
							Elements tds = tr.select("td");
							if(ths.size()>0 && tds.size()>=ths.size())
								localts._type = TableType.Column;
							else if(ths.size()>0)
								localts._type = TableType.Row;
							else
								localts._type = TableType.Layout;
							break;
						}
					}
				}
			}
		}
		return localts;
	}
		
	/**
	 * Returns the datatypes for the given table content. Assumes table is row-oriented.
	 */
	private static HashSet<String> dataTypeOfContent(Element table){
		HashSet<String> datatypes = new HashSet<String>();
		Elements trs = table.select("tr");
		for(Element tr:trs){
			if(tr==trs.first() && tr.select("th").size()>0)
				continue;
			Elements tds = tr.select("td");
			if(tds.size()>0){
				for(Element td:tds){
					if(td.html().equalsIgnoreCase("&nbsp;") || td.html().matches("/^$|\\s+/") )
						continue;
					try{
						Boolean flag = Boolean.parseBoolean(td.html());
						if(flag)
							datatypes.add("Boolean");
						else
							throw new Exception();
					}catch(Exception bool){
						try{
							Long.parseLong(td.html());
							datatypes.add("Int/Long");
						}catch(Exception intLong){
							try{
								Double.parseDouble(td.html());
								datatypes.add("Float/Double");
							}catch(Exception floatDouble){
								datatypes.add("String");
							}
						}
					}
				}
			}
		}
		
		return datatypes;
	}
	
}
