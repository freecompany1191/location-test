package com.o2osys;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.o2osys.common.CommonUtils;
import com.o2osys.component.LocationKakaoComponent;
import com.o2osys.entity.kakao.Location;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LocationTestApplicationTests {
	
	private final Logger log = LoggerFactory.getLogger(LocationTestApplicationTests.class);
	
	// 다음지도 컴포넌트
	@Autowired
	private LocationKakaoComponent lc;
	
	@Autowired
	CommonUtils util;
		
	@Test
	public void contextLoads() throws Exception {
		
		// 카카오 api를 통한 좌표 조회
		
		Location location = lc.getAddr("서울특별시 노원구 월계1동 389-1 삼능스페이스향 804호");
		
		log.debug("#### location : "+ util.jsonStringFromObject(location));
		
	}

}
