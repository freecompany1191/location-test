package com.o2osys.component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.o2osys.entity.oldLocation;
import com.o2osys.Define.*;
import com.o2osys.common.CommonUtils;
import com.o2osys.entity.daum.ResAddr2coord;
import com.o2osys.entity.daum.ResAddr2coord.Channel.Item;
import com.o2osys.entity.daum.ResKeyword;

@Component
public class LocationComponent {
	private final String TAG = LocationComponent.class.getSimpleName();
	// 로그
	private final Logger LOGGER = LoggerFactory.getLogger(LocationComponent.class);

	private final int TIMEOUT = 5;

	private final Pattern OLD_ADDR = Pattern.compile("\\(구\\s[가-힣0-9]*\\)", Pattern.DOTALL);
	
	private final Pattern SIGUGUN =  Pattern.compile("(([가-힣]+(시|도)|bc|서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|경북|경남|제주)\\s[가-힣]+(시|군|구).*)");

	private final ObjectMapper mObjectMapper = new ObjectMapper();

	@Autowired
	CommonUtils util;
	
	@Value("${daum.api.addr2coord.url}")
	String mAddr2coordUrl;

	@Value("${daum.api.keyword.url}")
	String mKeywordUrl;

	@Value("${daum.apikey}")
	String[] mDaumApikey;

	@PostConstruct
	public void init() {
	}

	@PreDestroy
	public void destroy() {
	}

	public oldLocation getAddr(String address) throws Exception {
		
		LOGGER.debug("@@@ Before address : "+ address);
		address = replaceOldAddr(address);
		LOGGER.debug("@@@ After address : "+ address);
		
		oldLocation location = getAddr2coord(address);

		if (location != null) {
			return location;
		}

		return getKeyword(address);
	}

	private oldLocation getAddr2coord(String address) throws Exception {
		LOGGER.debug("### address = "+address);
		String url = mAddr2coordUrl + "?apikey=" + getApikey() + "&q=" + URLEncoder.encode(address, "UTF-8")
				+ "&output=json";

		RequestConfig config = RequestConfig.custom().setConnectTimeout(TIMEOUT * 1000)
				.setConnectionRequestTimeout(TIMEOUT * 1000).setSocketTimeout(TIMEOUT * 1000).build();

		HttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("Content-type", "application/x-www-form-urlencoded");
		HttpResponse response = httpclient.execute(httpGet);

		switch (response.getStatusLine().getStatusCode()) {
		case HttpStatus.SC_OK:
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

			StringBuilder sbBuilder = new StringBuilder();
			String line = "";
			while ((line = bufferedReader.readLine()) != null) {
				sbBuilder.append(line);
			}
			LOGGER.debug("[RESPONSE BODY[" + sbBuilder.toString() + "]");
			LOGGER.debug("[" + url + "] RESPONSE BODY[" + sbBuilder.toString() + "]");

			ResAddr2coord resAddr2coord = mObjectMapper.readValue(sbBuilder.toString(), ResAddr2coord.class);
			
			LOGGER.info("#### resAddr2coord:" + resAddr2coord);

			if (resAddr2coord != null && resAddr2coord.getChannel() != null
					&& resAddr2coord.getChannel().getItems() != null
					&& resAddr2coord.getChannel().getItems().size() > 0) {
				Item item = resAddr2coord.getChannel().getItems().get(0);
				if (item != null) {
					oldLocation location = new oldLocation();
					location.setLatitude(String.valueOf(item.getLat()));
					location.setLongitude(String.valueOf(item.getLng()));
					location.setXyAccType(XyAccType.TYPE_1);

					if ("Y".equals(item.getIsNewAddress())) {
						location.setNewAddr(item.getTitle());
						if (!StringUtils.isEmpty(item.getNewAddress())) {
							
							//주소가 여러개 일때 첫번째 주소만 가져옴.2017.07.21 추가
							String newAddress = item.getNewAddress();
							if(newAddress.indexOf("|") != -1){
								newAddress = StringUtils.split(item.getNewAddress(), "|")[0];
								LOGGER.debug("주소 보완 [" + newAddress + "]");
							}
							
							location.setOldAddr(
									item.getLocalName1() + " " + item.getLocalName2() + " " + newAddress);
						} else {
							location.setOldAddr("");
						}
					} else {
						location.setOldAddr(item.getTitle());
						if (!StringUtils.isEmpty(item.getNewAddress())) {
							
							//주소가 여러개 일때 첫번째 주소만 가져옴.2017.07.21 추가
							String newAddress = item.getNewAddress();
							if(newAddress.indexOf("|") != -1){
								newAddress = StringUtils.split(item.getNewAddress(), "|")[0];
								LOGGER.debug("주소 보완 [" + newAddress + "]");
							}
							
							location.setNewAddr(
									item.getLocalName1() + " " + item.getLocalName2() + " " + newAddress);
						} else {
							location.setNewAddr("");
						}
					}

					location.setLocalName1(item.getLocalName1());
					location.setLocalName2(item.getLocalName2());
					location.setLocalName3(item.getLocalName3());

					return location;
				}
			}

			return null;
		case DaumHttpStatus.Code.TOO_MANY_REQUEST:
			LOGGER.error("[" + url + "] STATUS CODE[" + response.getStatusLine().getStatusCode() + "]");

			throw new Exception("[" + url + "] STATUS CODE[" + response.getStatusLine().getStatusCode() + "]");
		default:
			LOGGER.error("[" + url + "] STATUS CODE[" + response.getStatusLine().getStatusCode() + "]");

			return null;
		}
	}

	private oldLocation getKeyword(String address) throws Exception {
		return getKeyword(0, address);
	}

	private oldLocation getKeyword(int num, String address) throws Exception {
		LOGGER.debug("#### getKeyword["+num+"] address = "+address);
		if (StringUtils.isEmpty(address)) {
			return null;
		}

		//주소 셋팅
		String url = mKeywordUrl + "?apikey=" + getApikey() + "&query=" + URLEncoder.encode(address, "UTF-8");

		//소켓 설정
		RequestConfig config = RequestConfig.custom().setConnectTimeout(TIMEOUT * 1000)
				.setConnectionRequestTimeout(TIMEOUT * 1000).setSocketTimeout(TIMEOUT * 1000).build();

		HttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		
		//URL 셋팅
		HttpGet httpGet = new HttpGet(url);
		//헤더 셋팅
		httpGet.setHeader("Content-type", "application/x-www-form-urlencoded");
		HttpResponse response = httpclient.execute(httpGet);

		
		switch (response.getStatusLine().getStatusCode()) {
		case HttpStatus.SC_OK:
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

			StringBuilder sbBuilder = new StringBuilder();
			String line = "";
			while ((line = bufferedReader.readLine()) != null) {
				sbBuilder.append(line);
			}
			LOGGER.info("#### getKeyword["+num+"] response:" + sbBuilder.toString());
			LOGGER.info(url + " response:" + sbBuilder.toString());

			ResKeyword resKeyword = mObjectMapper.readValue(sbBuilder.toString(), ResKeyword.class);

			LOGGER.info("#### getKeyword["+num+"] resKeyword:" + resKeyword);
			if (resKeyword != null && resKeyword.getChannel() != null && resKeyword.getChannel().getItems() != null
					&& resKeyword.getChannel().getItems().size() > 0) {

				ResKeyword.Channel.Item item = resKeyword.getChannel().getItems().get(0);
				if (item != null) {
					oldLocation location = new oldLocation();
					location.setLatitude(item.getLatitude());
					location.setLongitude(item.getLongitude());
					if (num == 0) {
						location.setXyAccType(XyAccType.TYPE_2);
					} else {
						location.setXyAccType(XyAccType.TYPE_3);
					}

					location.setOldAddr("");
					location.setNewAddr("");

					location.setLocalName1("");
					location.setLocalName2("");
					location.setLocalName3("");

					return location;
				}
			}

			break;
		case DaumHttpStatus.Code.TOO_MANY_REQUEST:
			LOGGER.error("[" + url + "] STATUS CODE[" + response.getStatusLine().getStatusCode() + "]");

			throw new Exception("[" + url + "] STATUS CODE[" + response.getStatusLine().getStatusCode() + "]");
		default:
			LOGGER.error("[" + url + "] STATUS CODE[" + response.getStatusLine().getStatusCode() + "]");

			break;
		}

		address = nextAddr(address);

		if (!StringUtils.isEmpty(address)) {
			return getKeyword(num + 1, address);
		}

		return null;
	}

	/**
	 * 정규식 제거
	 * 
	 * @param address
	 * @return
	 */
	private String replaceOldAddr(String address) {
		Matcher matcher = OLD_ADDR.matcher(address);
		LOGGER.debug("#### matcher = "+matcher);
		
		if (matcher.find()) {
			LOGGER.debug("replaceOldAddr matcher.find()");
		}

		return matcher.replaceAll("");
	}
	
	private static String getMatch(Pattern p, String target){
	    
		String result = target;
		
		Matcher m = p.matcher(target);
	    System.out.println("### matcher = "+m);
	
	    if (m.find()) result=m.group();
	      
		return result;
	}


	/**
	 * 다음 주소를 가져온다.
	 */
	private String nextAddr(String address) {
		if (address.length() < 10) {
			return null;
		}

		String[] temp = address.split(" ");
		
		for (int i = 0; i < temp.length ; i++) {
			LOGGER.debug("### temp["+i+"] = "+temp[i]);
		}

		if (temp == null || temp.length <= 3) {
			return null;
		}

		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < temp.length - 1; i++) {
			stringBuilder.append(temp[i]);
			stringBuilder.append(" ");
		}

		return stringBuilder.toString();
	}

	/**
	 * api key 를 가져온다.
	 * 
	 * @return
	 */
	private String getApikey() {
		if (mDaumApikey == null || mDaumApikey.length <= 0) {
			return "";
		}

		Random random = new Random();
		int seq = random.nextInt(mDaumApikey.length);

		return mDaumApikey[seq];
	}
}
