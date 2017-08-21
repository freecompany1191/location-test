package com.o2osys;

import java.net.URI;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.o2osys.component.LocationComponent;
import com.o2osys.entity.kakao.Response.ResAddress;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LocationTestApplicationTests {
	
	private final Logger log = LoggerFactory.getLogger(LocationTestApplicationTests.class);
	
	// 다음지도 컴포넌트
	@Autowired
	private LocationComponent lc;
		
	private final Pattern SIGUGUN =  Pattern.compile("(([가-힣]+(시|도)|bc|서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|경북|경남|제주)\\s[가-힣]+(시|군|구).*)");
	
	//Authorization
	String token = "zpynTKRPs0kHyjE280KeAQkysXKvK-ZEP0kSXAopdaYAAAFdyrkdHA";
	String key = "KakaoAK ede6a39a917ebdbb4d1a1fb6f6bd7eff";
	String key2 = "KakaoAK ff163f8e19f73c100ad96c5eec1c618b";
	String url = "https://dapi.kakao.com/v2/local/search/address.json";
	
	@Test
	public void contextLoads() throws Exception {
		
		String query = URLEncoder.encode("역곡동", "UTF-8");
		
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		headers.add("Authorization",key2);
		
		String addr = getMatch(SIGUGUN, "난곡동 (구 신림3동+신림13동) 서울시 관악구 난곡로31길 32-4 현대빌라 101호");
		log.debug("#### addr : "+ addr);
		
		//UriComponents
		URI uri = UriComponentsBuilder.fromHttpUrl(url)
				.queryParam("query", addr)
				.build()
				.encode("UTF-8")
				.toUri();
			
		log.debug("### uri : "+uri.toString());
		RestTemplate restTemplate = new RestTemplate();
		
		//ReqAddress params = new ReqAddress();
		//params.setQuery(URLEncoder.encode("역곡동", "UTF-8"));
		
		//Map<String, Object> args = new HashMap<String, Object>();
		//args.put("query", "역곡동");
		
		HttpEntity<?> entity = new HttpEntity<>(headers);
		
		log.debug(jsonStringFromObject(entity));
		
		//log.debug("URI = "+uri);
		
		
		//url = url+"?query="+URLEncoder.encode("역곡동", "UTF-8");
		
		log.debug("URL = " + url);
//		ResponseEntity<ResAddress> resMsg = 
		ResponseEntity<ResAddress> resMsg = restTemplate.exchange(uri, HttpMethod.GET, entity, ResAddress.class);
		
		log.debug("### ALL = "+resMsg.getBody());
		log.debug("### ALL = "+jsonStringFromObject(resMsg));
		log.debug("### body = "+jsonStringFromObject(resMsg.getBody()));
		
		
		// 다음 api를 통한 좌표 조회
		/*
		Location location = lc.getAddr("난곡동 (구 신림3동+신림13동) 서울시 관악구 난곡로31길 32-4 현대빌라 101호");
		
		if (location == null) {
			location = new Location();
		}
		
		log.debug("#### location.toString() : "+ location.toString());
		*/
	}
	
	//패턴 적용
	private static String getMatch(Pattern p, String target){
	    
		String result = target;
		
		Matcher m = p.matcher(target);
	    System.out.println("### matcher = "+m);
	
	    if (m.find()) result=m.group();
	      
		return result;
	}

	/**
     * Object -> JSON 형식으로 변환
     * 
     * @Method Name : jsonStringFromObject
     * @param object
     * @return
     * @throws JsonProcessingException
     */
    public static String jsonStringFromObject(Object object) throws JsonProcessingException {
        
        if(object == null){
            return null;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

}
