package com.o2osys.component;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.o2osys.Define.XyAccType;
import com.o2osys.common.CommonUtils;
import com.o2osys.entity.kakao.Location;
import com.o2osys.entity.kakao.Response.ResAddress;
import com.o2osys.entity.kakao.Response.ResCoord2address;
import com.o2osys.entity.kakao.Response.ResAddress.Documents;
import com.o2osys.entity.kakao.Response.ResKeyword;
import com.o2osys.common.constants.*;
import com.o2osys.common.constants.ADDRESS_PATTERN.STEP;

/**
   @FileName  : LocationKakaoComponent.java
   @Description : 
   @author      : KMS
   @since       : 2017. 8. 22.
   @version     : 1.0
  
   @개정이력
   
   수정일           수정자         수정내용
   -----------      ---------      -------------------------------
   2017. 8. 22.     KMS            최초생성
 
 */
@Component
public class LocationKakaoComponent {
	private final String TAG = LocationKakaoComponent.class.getSimpleName();
	// 로그
	private final Logger log = LoggerFactory.getLogger(LocationKakaoComponent.class);

	@Autowired
	CommonUtils util;
	
	@Value("${kakao.restapi.key}")
	String KAKAO_API_KEY;
	
	@Value("${kakao.api.url.address}")
	String ADDRESS_URL;
	
	@Value("${kakao.api.url.keyword}")
	String KEYWORD_URL;
	
	@Value("${kakao.api.url.coord2address}")
	String COORD2ADDRESS_URL;
	
	@PostConstruct
	public void init() {
	}

	@PreDestroy
	public void destroy() {
	}

	/**
	 * 조회된 주소 및 향상된 주소 가져오기
	 * @Method Name : getAddr
	 * @param address
	 * @return
	 * @throws Exception
	 */
	public Location getAddr(String address) throws Exception {
		
		String oldAddress = address;
		log.info("@@ 주소 기본패턴 적용 전 Before Address : "+ address);
		address = getMatchDefault(address); //시군구 패턴을 1차적으로 걸러냄
		log.info("@@ 주소 기본패턴 적용 후 After Address : "+ address);
		//주소 향상기능 모듈적용
		Location location = addrAccInc(address);
		location.setEaAddr7(oldAddress);
		
		return location;
	}

	
	/**
	 * 단계별 패턴을 적용하여 주소 향상기능 호출
	 * @Method Name : addrApiRoop
	 * @param address
	 * @return
	 * @throws Exception
	 */
	private Location addrAccInc(String address) throws Exception{
		
		Location loc = null;
		Location old_loc = null;
		
		String searchType = "address";
		log.info("@@ KAKAO API 주소 검색 시작 : "+address);
		loc = addrApiRoop(searchType, address);
		
		if(loc != null && loc.getXyAccType().equals(XyAccType.TYPE_1)){
			log.info("@@ KAKAO API 주소 정확도 높음으로 리턴 : "+loc.getAddressName());
			return loc;
		}
		 
		log.info("@@ KAKAO API 키워드 검색 시작 : "+address);
		old_loc = loc;
		if(old_loc != null){
			log.info("@@ KAKAO API 기존 주소 타입 : "+old_loc.getAddressType()+" | 조회결과 : "+old_loc.getAddressName()+" | 정확도 : "+old_loc.getXyAccType());
		}
		
		searchType = "keyword";
		loc = addrApiRoop(searchType, address);
		
		if(loc != null){
			log.info("@@ KAKAO API 키워드 주소 타입 : "+loc.getAddressType()+" | 조회결과 : "+loc.getAddressName()+" | 정확도 : "+loc.getXyAccType());
			
			if(old_loc !=null){
				log.info("@@ KAKAO API 키워드 주소 정확도 비교 : 기존 주소 = "+old_loc.getXyAccType()+" > 키워드 주소 = "+loc.getXyAccType()+" = "
				+( Integer.valueOf((old_loc.getXyAccType())) > Integer.valueOf((loc.getXyAccType())) ) );
				
				if(Integer.valueOf((old_loc.getXyAccType())) > Integer.valueOf((loc.getXyAccType()))){
					log.info("@@ KAKAO API 키워드 주소 정확도가 높음으로 키워드 주소 리턴 : "+loc.getAddressName());
					return loc;
				}
			}
			
			if(loc != null && loc.getXyAccType().equals(XyAccType.TYPE_1)){
				log.info("@@ KAKAO API 기존주소 없음 키워드 주소 정확도 높음으로 리턴 : "+loc.getAddressName());
				return loc;
			}
			
			//log.debug("### 기존 주소 정확도가 높음으로 기존 주소 리턴 : "+old_loc.getAddressName());
			//return old_loc;
		}
		
		int pattern_num = 0;
		
		log.info("@@ KAKAO API 매칭할 패턴 갯수 : "+ADDRESS_PATTERN.STEP.values().length);
		for(STEP step : ADDRESS_PATTERN.STEP.values()){
			
			String addressPM = getMatchOut(step.getPettern(),address);
			log.info("@@ KAKAO API 패턴매치["+pattern_num+"] 주소 체크 : "+addressPM);
			
			if(!addressPM.equals(address)){
				log.info("@@ KAKAO API 패턴매치["+pattern_num+"] 수행 시작 ... : ["+!addressPM.equals(address)+"]");
				loc = addrApiRoop(searchType, addressPM);
				
				if(loc != null){
					log.info("@@ KAKAO API 패턴매치["+pattern_num+"] 주소 타입 : "+loc.getAddressType()+" | 조회결과 : "+loc.getAddressName()+" | 정확도 : "+loc.getXyAccType());
					
					if(old_loc !=null){
						log.info("@@ KAKAO API 정확도 비교 : 기존 주소 = "+old_loc.getXyAccType()+" > 패턴매치["+pattern_num+"] 주소 = "+loc.getXyAccType()+" = "
						+( Integer.valueOf((old_loc.getXyAccType())) > Integer.valueOf((loc.getXyAccType())) ) );
						
						if(Integer.valueOf((old_loc.getXyAccType())) > Integer.valueOf((loc.getXyAccType()))){
							log.info("@@ KAKAO API 패턴매치["+pattern_num+"] 주소 정확도가 높음으로 패턴매치["+pattern_num+"] 주소 리턴 : "+loc.getAddressName());
							return loc;
						}
					}
					
					if(loc != null && loc.getXyAccType().equals(XyAccType.TYPE_1)){
						log.info("@@ KAKAO API 기존주소 없음 패턴매치["+pattern_num+"] 주소 정확도 높음으로 리턴 : "+loc.getAddressName());
						return loc;
					}
				}
				
			}
		}
		
		log.info("@@ KAKAO API 검색 결과 없음 최종 조회 값 리턴 : "+old_loc);
		//REGION or ROAD or REGION_ADDR or ROAD_ADDR
		//"address_type": "REGION" 지명이면 낮음, "road_address": null 이면 낮음
		//"address_type": "ROAD" 도로명이면 낮음
		return old_loc;
	}
	
	
	/**
	 * 패턴이 적용된 주소를 반복하여 정확도 향상된 주소 추출
	 * @Method Name : addrApiRoop
	 * @param searchType
	 * @param address
	 * @return
	 * @throws Exception
	 */
	private Location addrApiRoop(String searchType, String address) throws Exception{
		
		Location loc = new Location();
		Location old_loc = null;
		
		int num = 0;
		
		do{
			loc = getAddress(num, searchType, address);
			
			if (loc != null) {
				
				//주소 정확도가 높음이면 리턴
				if(loc.getXyAccType().equals(XyAccType.TYPE_1)){
					return loc;
				}
				//주소 정확도가 높지 않을때
				else{
					//주소 정확도가 낮음일때
					if(loc.getXyAccType().equals(XyAccType.TYPE_3)){
						if(old_loc != null){
							//이전 주소 정확도가 중간이면 중간 주소를 리턴
							if(old_loc.getXyAccType().equals(XyAccType.TYPE_2)){
								return old_loc;
							}
						}
					}
					
					//주소는 있으나 정확도가 높지 않을때는 주소를 이전 주소로 저장
					old_loc = loc;
				}
				
			}
			
			//공백기준으로 뒷쪽 한블럭 제거 후 주소가 null 일때까지 다시 반복
			if(searchType.equals("address")){
				address = nextAddr(address);
				log.debug("## Address nextAddr = "+address);
			}else{
				address = nextAddr(address, 7);
				log.debug("## Keyword nextAddr = "+address);
			}
			num++;
			
			log.info("@@ KAKAO API ["+searchType+"] 검색 결과 없음 재시도["+num+"] = "+address);
			
		}while(address != null);
		
		
		log.info("@@ KAKAO API ["+searchType+"] 최종 검색 결과 없음 초기 저장 주소 리턴 = "+old_loc);
		//주소가 없을때까지 반복했는데도 정확도가 높지 않을때는 최근에 조회되었던 주소를 리턴
		return old_loc;
		
	}
	

	/**
	 * 패턴 매칭 주소값 가져오기 수행
	 * @Method Name : getAddress
	 * @param num
	 * @param searchType
	 * @param address
	 * @return
	 * @throws Exception
	 */
	private Location getAddress(int num, String searchType, String address) throws Exception {
		Location loc = new Location();
		ResAddress res = new ResAddress();
		
		log.debug("#### getKeyword["+num+"] address = "+address);
		
		if (StringUtils.isEmpty(address)) return null;
		
		res = RestTranAddress(searchType, address);
		
		//조회된 주소가 없으면 null을 리턴 하여 반복수행
		if(res.getMeta().getTotalCount() == 0) return null;
			
		//조회된 주소가 있을때 정확도 셋팅하여 리턴
		else if(res.getMeta().getTotalCount() >= 1){
			
			Documents doc  = res.getDocuments().get(0);
			loc = new Location(doc);
			
			//REGION or ROAD or REGION_ADDR or ROAD_ADDR
			//"address_type": "REGION" 지명이면 낮음, "road_address": null 이면 낮음
			//"address_type": "ROAD" 도로명이면 낮음//REGION 지명
			
			//조회된 주소 카운트가 1이면 정확도 높음
			if(res.getMeta().getTotalCount() == 1)
				loc.setXyAccType(XyAccType.TYPE_1);
			//조회된 주소 카운트가 1보다 크면 정확도 중간
			else if(res.getMeta().getTotalCount() > 1)
				loc.setXyAccType(XyAccType.TYPE_2);
			//조회된 주소 카운트가 10보다 크면 정확도 낮음
			else if(res.getMeta().getTotalCount() > 5)
				loc.setXyAccType(XyAccType.TYPE_3);
			//그외 정확도 낮음
			else
				loc.setXyAccType(XyAccType.TYPE_3);

			//주소 타입 확인
			switch(doc.getAddressType()){
			case "REGION" : //주소 타입이 지명이면 정확도 낮음
				loc.setXyAccType(XyAccType.TYPE_3);
				break;
			case "ROAD" : //주소 타입이 도로명이면 정확도 낮음
				loc.setXyAccType(XyAccType.TYPE_3);
				break;
			}
			
			//도로명 주소가 null 이면 정확도 낮음
			if(doc.getRoadAddress() == null)
				loc.setXyAccType(XyAccType.TYPE_3);
			
		}
		
		return loc;
	}
	
	
	/**
	 * 카카오 API 로컬 Address를 REST로 호출
	 * @Method Name : RestTranAddress
	 * @param searchType
	 * @param address
	 * @return
	 * @throws Exception
	 */
	private ResAddress RestTranAddress(String searchType, String address) throws Exception {
		ResAddress res = new ResAddress(); 
		
		log.debug("### address = "+address);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		headers.add("Authorization",KAKAO_API_KEY);
		
		RestTemplate restTemplate = new RestTemplate();
		HttpEntity<?> entity = new HttpEntity<>(headers);
		ResponseEntity<ResAddress> resMsg = null;
		
		//검색 타입별 분기
		switch(searchType){
			case "address": //주소검색 일때
				
				resMsg = restTemplate.exchange(getUri(ADDRESS_URL, address), HttpMethod.GET, entity, ResAddress.class);
				
				break;
				
			case "keyword": //키워드검색일때
				
				ResKeyword.Documents docK = (ResKeyword.Documents) getDocument(ResKeyword.class, getUri(KEYWORD_URL, address), entity);
				
				if(docK != null){
					
					log.debug("# road_address_name : "+docK.getRoadAddressName()+" | address_name : "+docK.getAddressName()+" | place_name : "+docK.getPlaceName());
					
					//ResponseEntity<ResCoord2address> resCoord2address = null;
					
					String query = null;
					
					//키워드 검색으로 조회된 주소를 담는다
					//도로명 주소가 없는 경우에는 정확도가 낮으므로 도로명 주소를 기준으로 검색한다
					if(!StringUtils.isEmpty(docK.getRoadAddressName())){ //키워드 도로명 주소가 있으면 query에 담는다
						
						query = docK.getRoadAddressName();
						
					} else { //키워드 도로명 주소가 없으면 좌표->주소 호출
						
						//좌표->주소 검색 호출
						ResCoord2address.Documents docC2A = (ResCoord2address.Documents) getDocument(ResCoord2address.class, getUri(COORD2ADDRESS_URL, docK.getLongitude(), docK.getLatitude()), entity);
						
						//좌표->주소 검색 결과가 있으면
						if(docC2A != null){
							
							if(docC2A.getRoadAddress() != null && !StringUtils.isEmpty(docC2A.getRoadAddress().getAddressName())) //좌표->주소 도로명 주소가 있으면 query에 담는다
								query = docC2A.getRoadAddress().getAddressName();
							else if(docC2A.getAddress() != null && !StringUtils.isEmpty(docC2A.getAddress().getAddressName())) //좌표->주소 도로명 주소가 있으면 query에 담는다
								query = docC2A.getAddress().getAddressName();
						
						}
						
					}
					
					//query 담긴 주소가 있으면 주소 검색 호출
					if(!StringUtils.isEmpty(query))
						resMsg = restTemplate.exchange(getUri(ADDRESS_URL, query), HttpMethod.GET, entity, ResAddress.class);
					
				}
				break;
		}
		
		//주소 검색이 호출되어 Response 데이터가 있을 때
		if(resMsg != null){
			log.debug("[ADDRESS RESPONSE BODY] = "+util.jsonStringFromObject(resMsg.getBody()));
			
			if(resMsg.getStatusCode() == HttpStatus.OK) {
			
				res.setMeta(resMsg.getBody().getMeta());
				res.setDocuments(resMsg.getBody().getDocuments());
				
			}
		}else{ //주소 검색조건에 맞지 않았을 경우 기본 초기 데이터를 입력하여 리턴
			
			ResAddress.Meta addressMeta = new ResAddress.Meta();
			
			addressMeta.setTotalCount(0);
			addressMeta.setPageableCount(0);
			addressMeta.setEnd(true);
			
			res.setMeta(addressMeta);
		}

		log.debug("[RESPONSE BODY] = "+util.jsonStringFromObject(res.getMeta()));
		
		return res;
	}
		
		
	
	/**
	 * 주소 검색 이외 각 패턴별 API 호출
	 * @Method Name : getDocument
	 * @param cls
	 * @param uri
	 * @param entity
	 * @return
	 * @throws JsonProcessingException
	 */
	private Object getDocument(Class cls, URI uri, HttpEntity<?> entity) throws JsonProcessingException{
		
		RestTemplate rest = new RestTemplate();
		//cls.cast(obj)
		//키워드 검색으로 주소 호출
		ResponseEntity res = rest.exchange(uri ,HttpMethod.GET, entity, cls);
		
		//키워드 검색 통신 성공일때
		if(res.getStatusCode() == HttpStatus.OK) {
			
			log.debug("# ClassName ["+cls.getSimpleName()+"]");
			switch(cls.getSimpleName()){
				
				case "ResKeyword" :
					log.debug("[KEYWORD RESPONSE BODY] = "+util.jsonStringFromObject((ResKeyword) res.getBody()));
					ResKeyword.Meta metaK = ((ResKeyword) res.getBody()).getMeta();
					
					//키워드 검색 결과가 있을때
					if(metaK.getTotalCount() != null && metaK.getTotalCount() > 0)
						return ((ResKeyword) res.getBody()).getDocuments().get(0);
					
					break;
				
				case "ResCoord2address" :
					log.debug("[Coord2address RESPONSE BODY] = "+util.jsonStringFromObject((ResCoord2address) res.getBody()));
					ResCoord2address.Meta metaC2A = ((ResCoord2address) res.getBody()).getMeta();
					
					//키워드 검색 결과가 있을때
					if(metaC2A.getTotalCount() != null && metaC2A.getTotalCount() > 0)
						return ((ResCoord2address) res.getBody()).getDocuments().get(0);
					
					break;
			}
			
		}
		
		return null;
			
	}
	
	
	/**
	 * URI 가져오기
	 * @Method Name : getUri
	 * @param url
	 * @param query
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private URI getUri(String url, String query) throws UnsupportedEncodingException{
		
		URI uri=UriComponentsBuilder.fromHttpUrl(url)
				.queryParam("query", query)
				.build()
				.encode("UTF-8")
				.toUri();
		
		log.debug("# getUri : "+uri.toString());
		
		return uri;
	}
	
	
	/**
	 * URI 가져오기(좌표->주소)
	 * @Method Name : getUri
	 * @param url
	 * @param x
	 * @param y
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private URI getUri(String url, String x, String y) throws UnsupportedEncodingException{
		
		URI uri=UriComponentsBuilder.fromHttpUrl(url)
				.queryParam("x", x)
				.queryParam("y", y)
				.build()
				.encode("UTF-8")
				.toUri();
		
		log.debug("# getUri : "+uri.toString());
		
		return uri;
	}
	
	/**
	 * 기본 정규식 패턴 매칭
	 * @Method Name : getMatch
	 * @param p
	 * @param target
	 * @return
	 */
	private String getMatchDefault(String target){
	    
		String result = target;
		
		//기본 시군구 패턴 적용
		Pattern p = ADDRESS_PATTERN.DEFAULT.SIGUGUN.getPettern();
		
		Matcher m = p.matcher(target);
	
	    if (m.find()) result=m.group();
	    
	    Matcher m2;
	    //기본 동번지 패턴 적용
	    Pattern p2 = ADDRESS_PATTERN.DEFAULT.DONG_BUNGI.getPettern();
	    Matcher m3;
	    //기본 번지 패턴 적용
	    Pattern p3 = ADDRESS_PATTERN.DEFAULT.BUNGI.getPettern();
	    
	    //OO동999-999 형식의 문자열 분리를 위한 스트링배열
	    String[] tempArr=new String[3];
	    do{
    		m2 = p2.matcher(result); //OO동999-999 형식 패턴적용
    		if(m2.find()){ //OO동999-999 형식이 있을 경우
	    		tempArr[0] = m2.group(); //tempArr[0]에 담는다
	    		m3 = p3.matcher(tempArr[0]); //999-999 번지 형식 패턴적용
	    		if(m3.find()){ //999-999 번지 형식이 있을 경우
		    		tempArr[1] = m3.group(); //tempArr[1]에 담는다
		    		//tempArr[0]에 담은 값의 999-999 번지 형식값을 한칸 띄워서 동뒤에 붙임
		    		tempArr[2] = tempArr[0].replaceAll(tempArr[1]," "+tempArr[1]);
		    		//OO동999-999 형식을 OO동 999-999 형식으로 변경
		    		result = result.replaceAll(tempArr[0], tempArr[2]);
	    		}
    		}
	    }while(m.find());
	    
	    //중복동 제거 패턴 적용
	    p = ADDRESS_PATTERN.DEFAULT.DUPLE_DONG.getPettern();
	    
	    int i = 0;
	    int cnt = 0;
	    String chkStr = result;
	    
	    //돌면서 동 갯수 체크하기 위한 루프문
	    do{
    		m = p.matcher(chkStr);
    		if(m.find()){
	    		chkStr = chkStr.replace(m.group(), "");
	    		if(i!=0) cnt++; //첫번째 동은 갯수에서 제외 두번째 동부터 cnt 증가
    		}
    		i++;
    		
    	} while(m.find());
	    
	    //cnt 가 1이면 동이 2개이므로 cnt가 1개 이상일 때
	    if(cnt >= 1){
	    	
	    	i = 0;
	    	int cnt2 = 0; //
	    	String tempStr = result; //임시 주소 저장
		    String[] temp =new String[cnt]; //제거용 배열 생성 및 동 갯수만큼 배열크기 지정
		   
		    do{
		    	
	    		m = p.matcher(tempStr);
	    		if(m.find()){
	    			//돌면서 동이 발견되면 제거
		    		tempStr = tempStr.replace(m.group(), ""); //
		    		if(i!=0){
		    			//첫번째 동이 아니면 제거용 배열에 저장
		    			temp[cnt2] = m.group(); 
		    			cnt2++; //배열 카운트 증가
		    		}
	    		}
	    		i++;
	    		
	    	} while(m.find());
	
		    //최종 제거용 배열 크기 만큼 돌림
	    	for(int j = 0; j<temp.length;j++){
	    		//최종주소에서 제거용 배열에 들어있는 동을 모두 제거
	    		result = result.replaceAll(temp[j], "");
	    	}
	    
	    }
	      
		return result;
	}
	
	/**
	 * 정규식 패턴 매칭(매칭된 패턴과 일치한 것만 가져옴)
	 * @Method Name : getMatch
	 * @param p
	 * @param target
	 * @return
	 */
	private String getMatch(Pattern p, String target){
	    
		String result = target;
		
		Matcher m = p.matcher(target);
	
	    if (m.find()) result=m.group();
	      
		return result;
	}
	
	/**
	 * 정규식 패턴 매칭(매칭된 패턴 제거)
	 * @Method Name : getMatchOut
	 * @param p
	 * @param target
	 * @return
	 */
	private String getMatchOut(Pattern p, String target){
	    
		String result = target;
		
		Matcher m = p.matcher(target);
	      
	    result =  m.replaceAll(" ");
	    
		return result;
	}

	/**
	 * 뒤에서부터 공백을 기준으로 한블럭씩 잘라냄
	 * @Method Name : nextAddr
	 * @param address
	 * @return
	 */
	private String nextAddr(String address) {
		log.debug("# Address address.length() : "+address.length());
		if (address.length() < 3) {
			return null;
		}

		String[] temp = address.split(" ");
		
		/*
		for (int i = 0; i < temp.length ; i++) {
			log.debug("# temp["+i+"] = "+temp[i]);
		}
		
		log.debug("# temp splite length() : "+temp.length);
		*/
		
		if (temp == null || temp.length <= 1) {
			return null;
		}
		
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < temp.length - 1; i++) {
			stringBuilder.append(temp[i]);
			stringBuilder.append(" ");
		}

		return stringBuilder.toString().trim();
	}
	
	/**
	 * 뒤에서부터 공백을 기준으로 한블럭씩 잘라냄 길이 설정가능
	 * @Method Name : nextAddr
	 * @param address
	 * @return
	 */
	private String nextAddr(String address, int length) {
		log.debug("# Keyword address.length() : "+address.length());
		
		if (address.length() < length) {
			return null;
		}

		String[] temp = address.split(" ");
		
		/*
		for (int i = 0; i < temp.length ; i++) {
			log.debug("# temp["+i+"] = "+temp[i]);
		}
		
		log.debug("# temp splite length() : "+temp.length);
		*/
		
		if (temp == null || temp.length <= 2) {
			return null;
		}
		
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < temp.length - 1; i++) {
			stringBuilder.append(temp[i]);
			stringBuilder.append(" ");
		}

		return stringBuilder.toString().trim();
	}

}
