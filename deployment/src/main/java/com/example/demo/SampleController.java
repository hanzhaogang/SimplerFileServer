package com.example.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.ServletContextAware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@RestController
public class SampleController implements ServletContextAware{
    public static String uploadPathStr = "/home/geiri/shandong_deploy/"+"uploads"+"/"; 
    public static String labeledPathStr = "/home/geiri/shandong_deploy/"+"labeled"+"/";
    // The name of the file to open.
    public static String idFileName="id2Send.txt";
	private ServletContext context;
	private int maxSize = 102400 * 1024;// 102400KB以内(100MB)

	@Override
	public void setServletContext(ServletContext servletContext) {
		// TODO Auto-generated method stub
	    this.context = servletContext;
	}
	
    @RequestMapping("/")
    String home() {
        return "Hello This is the AI object detection service !";
    }
    
    @RequestMapping(value="/",method=RequestMethod.POST)
    String multipartPostTest() {
        return "test multi part post";
    }
    @RequestMapping(value="/",method=RequestMethod.PUT)
    String putTest() {
        return "test put";
    }

    /*
     * 1 receive the file and save it into the file system.
     * 2 response to the request.
     */
    @RequestMapping(value="/edit/FileUpload",method= RequestMethod.POST)
    String fileUpload(HttpServletRequest request) {
    	//MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        //MultipartFile file = multipartRequest.getFile("file");
        
        File uploadPath = new File(uploadPathStr);
        if (!uploadPath.exists()) {
        	uploadPath.mkdirs();
        }
        
        String idStr="";
        try  {
        	Part jsonPart=request.getPart("FileUpload");
        	InputStream input2 = jsonPart.getInputStream();
        	String jsonString = IOUtils.toString(input2, "utf-8"); 
        	//{"filename":"869721020145075_20170907071152.jpg",
        	//"filesize":1,
        	//"id":"869721020145075_20170907071152",
        	//"ip":"127.0.0.1",
        	//"port":0,
        	//"timestamp":1541651386}
			input2.close();
			JsonNode rootNode = new ObjectMapper().readTree(new StringReader(jsonString));
			JsonNode idFeild=rootNode.get("id");
			idStr=idFeild.asText();
            //jsonPart.getH
        	Part filePart=request.getPart("file");
        	File file = new File(uploadPath, idStr+".jpg");
        	InputStream input = filePart.getInputStream();
        	Files.copy(input, file.toPath());
        	System.out.println("upload file saved in the file system");
			input.close();
        } catch (IOException | ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
        }

        class Response{
        	public String id;
        	public int code;
        	public String message;
        }
        Response res=new Response();
        res.id=idStr;
        res.code=0;
        res.message="";
        return JsonUtil.toJson(res);
    }
    
    /*
     * return info of 10 labeled pictures
     */
    @RequestMapping(value="/edit/services/rest/edit/Images/Results",method=RequestMethod.GET)
    @ResponseBody
    String analysisResult(HttpServletRequest request) {
    	//
    	class Results{
    		public int code;
    		public String message;
    		public boolean hasMore;
    		public List<Photo> photos;
    	} 	

    	Results res=new Results();
    	res.code=0;
    	res.message="";
    	
    	//determine the hasMore attribute
        File uploadPath = new File(uploadPathStr);
        if (!uploadPath.exists()) {
        	uploadPath.mkdirs();
        }	
		Path file = Paths.get(labeledPathStr+"id2Send.txt");
    	try (Stream<String> lines = Files.lines(file, Charset.defaultCharset())) {
    	    long numOfLines = lines.count();
    	    if(10<numOfLines)
    	    	res.hasMore=true;
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	//get the first 10 id of the pictures
        int counter = 0;
        String line = null;
        FileReader fileReader = null;
        List<String> idList=new ArrayList<String>(); 
        BufferedReader bufferedReader;
        try {
            // FileReader reads text files in the default encoding.
            fileReader = new FileReader(labeledPathStr+idFileName);
            // Always wrap FileReader in BufferedReader.
            bufferedReader = new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                counter++;
                System.out.println("getting a id from the id2Send.txt file");
                idList.add(line.trim());
                if(counter == 10) {
                	break;
                }
            }   
            if(fileReader != null)
            	bufferedReader.close();  
        } catch(FileNotFoundException ex) {
            System.out.println( "Unable to open file '" + idFileName + "'");                
        } catch(IOException ex) {
            System.out.println( "Error reading file '" + idFileName + "'");                  
        }
        
        //now we construct the 10 photo objects from the idList
        List<Photo> photos=new ArrayList<Photo>();
        for(String id:idList) {
        	Photo photo=new Photo();
        	photo.id=id;
        	photo.ip="";
            photo.name=id;
            photo.port=0;
            photo.results=new ArrayList<Target>();
            photo.stamp=0;
            photo.timestamp=0;
	        String fileurl = "http://192.168.10.170/"+id+".jpg";
            photo.url=fileurl;
            photos.add(photo);
        }
        res.photos=photos;
        
        return JsonUtil.toJson(res);
    }
    
    @RequestMapping(value="/edit/services/rest/edit/Images/Results",method=RequestMethod.PUT)
    String res4AnalysisResult(HttpServletRequest req, PhotoIds PhotosRecvArgu) {
    	//get the id that the client has put to the request.
        List<String> idsToRemove = new ArrayList<String>();
    	try {
			String jsonStr = req.getReader().lines().collect(Collectors.joining(
					System.lineSeparator()));
			//System.out.println(jsonStr);
			JsonNode rootNode = new ObjectMapper().readTree(new StringReader(jsonStr));
			JsonNode photosIds=rootNode.get("PhotosRecvArgu");
			JsonNode array=photosIds.get("photoIds");
			System.out.println(array.getNodeType());
			Iterator<JsonNode> arrayEle=array.iterator();
			while(arrayEle.hasNext()) {
				JsonNode id=arrayEle.next();
				String idStr=id.asText();
				idsToRemove.add(idStr);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    	//delete these id from the id.txt file
        File inputFile = new File(labeledPathStr+"id2Send.txt");
        File tempFile = new File(labeledPathStr+"myTempFile.txt");
        BufferedReader reader;
        BufferedWriter writer;
        boolean successful=false;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(tempFile));
	        String currentLine;
	        while((currentLine = reader.readLine()) != null) {
	            // trim newline when comparing with lineToRemove
	            String trimmedLine = currentLine.trim();
	            if(idsToRemove.contains(trimmedLine)) 
	            	continue;
	            writer.write(currentLine + System.getProperty("line.separator"));
	        }
	        writer.close(); 
	        reader.close(); 
	        inputFile.delete();
	        successful = tempFile.renameTo(inputFile);  
	        if(successful)
	        	System.out.println("id2Sent.txt file updated, pictures client has received have been removed from the id2Send.txt");
	        else
	        	System.out.println("id2Sent.txt file updated failed!");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
        //return json response
        class Response{
        	public int code;
        	public String message;
        }
        Response res=new Response();
        if(!successful)
         res.code=1;
        res.message="";
        return JsonUtil.toJson(res);
    }
}


class PhotoIds{
	public List<String> photoIds;
}


class Photo{
	public String id;
	public String ip;
	public String name;
	public int port;
	public List<Target>results;
	public int stamp;
	public int timestamp;
	public String url;
}

class Target{
	public int h;
	public int w;
	public int x;
	public int y;
	public String objectType;
}

class JsonUtil{
	public static String toJson(Object obj) {
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    	String json="";
    	try {
			json = ow.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return json;    
	}
}

