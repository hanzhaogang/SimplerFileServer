package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.ServletContextAware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@RestController
public class SampleController implements ServletContextAware{
    
   
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
    	System.out.println("entering the fileUpload method.");
    	//MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        //MultipartFile file = multipartRequest.getFile("file");
        
        File uploadPath = new File("/home/share/matlab/MPSInstances/noHelmetPersonDetector/Step0_MyTestImages/");
        if (!uploadPath.exists()) {
        	uploadPath.mkdirs();
        }
        
        
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
			
            //jsonPart.getH
        	Part filePart=request.getPart("file");
        	File file = new File(uploadPath, "input.jpg");
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
        res.id="idStr";
        res.code=0;
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

