package com.o2osys.component;

import java.io.UnsupportedEncodingException;
import java.net.URI;
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
import com.o2osys.common.constants.ADDRESS_PATTERN;
import com.o2osys.common.constants.ADDRESS_PATTERN.STEP;
import com.o2osys.entity.kakao.Location;
import com.o2osys.entity.kakao.Response.ResAddress;
import com.o2osys.entity.kakao.Response.ResAddress.Documents;
import com.o2osys.entity.kakao.Response.ResCoord2address;
import com.o2osys.entity.kakao.Response.ResKeyword;

/**
   @FileName  : LocationKakaoComponent.java
   @Description : 카카오API 주소 검색 모듈
   @author      : KMS
   @since       : 2017. 8. 22.
   @version     : 1.0

   @개정이력

   수정일           수정자         수정내용
   -----------      ---------      -------------------------------
   2017. 8. 22.     KMS            최초생성
   2017. 9. 12.     KMS            패턴 추가 및 로직 보완
   2017. 9. 14.     KMS            패턴 추가 및 로직 보완(동갯수에 따른 루프문 처리)

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

    //동이 여러개일 경우 정확도를 높이기 위해 배열에 담아 재시
    String[] dongArr;

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

        //조회된 주소가 있을때만
        if(location != null){

            //동 배열에 값이 있고 정확도가 높지 않을때 동 배열에 담긴 동들로 루프를 돌리며 주소검색 재시도
            if(dongArr != null && !location.getXyAccType().equals(XyAccType.TYPE_1)){

                for(String netxDong : dongArr){
                    //현재주소의 동을 배열의 다음 동으로 변환
                    address = getMatchDongRoop(address, netxDong);
                    System.out.println("netxDong : "+netxDong+" | address : "+address);
                    //다음 동으로 변환된 주소로 다시 주소를 검색한다
                    location = addrAccInc(address);
                }

            }

            //배달 대행 업체에서 들어온 주소를 셋팅한다
            location.setEaAddr7(oldAddress);

        }

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

            String addressPM = getMatchOutBlank(step.getPettern(),address);
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

        Location loc = null;
        Location old_loc = null;
        String old_address = address;

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

            //검색할 주소가 시도 시군구 읍면동리가길로 까지만 있으면 BREAK
            if(getMatchXyAccType(ADDRESS_PATTERN.CUSTOM.SIDODONG_ONLY.getPettern() , address)){
                log.info("@@ KAKAO API ["+searchType+"] 시도 시군구 읍면동리가길로 BREAK ["+num+"] = "+address);
                break;
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

            //검색 주소가 시도 시군구 읍면동리가길로 까지만 있으면 정확도 낮음
            if(getMatchXyAccType(ADDRESS_PATTERN.CUSTOM.SIDODONG_ONLY.getPettern() , address))
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
                    log.debug("[KEYWORD RESPONSE BODY] = "+util.jsonStringFromObject(res.getBody()));
                    ResKeyword.Meta metaK = ((ResKeyword) res.getBody()).getMeta();

                    //키워드 검색 결과가 있을때
                    if(metaK.getTotalCount() != null && metaK.getTotalCount() > 0)
                        return ((ResKeyword) res.getBody()).getDocuments().get(0);

                    break;

                case "ResCoord2address" :
                    log.debug("[Coord2address RESPONSE BODY] = "+util.jsonStringFromObject(res.getBody()));
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

        //STEP1 - [문자] or (문자) or {문자} 제거
        target = getMatchRoopOut(ADDRESS_PATTERN.DEFAULT.BRACKET_STR.getPettern(), target);
        log.info("@@ 주소 기본패턴 STEP1 적용 : "+ target);

        //STEP2 - 특수문자 제거
        target = getMatchRoopOut(ADDRESS_PATTERN.DEFAULT.SPECIAL_STR.getPettern(), target);
        log.info("@@ 주소 기본패턴 STEP2 적용 : "+ target);

        //STEP3 - 시군구 패턴 적용
        target = getMatch(ADDRESS_PATTERN.DEFAULT.SIGUGUN.getPettern(), target);
        log.info("@@ 주소 기본패턴 STEP3 적용 : "+ target);

        //STEP4 - 중복 시도 시군구 읍면동가리로길 제거
        target = getMatchdupleOut(ADDRESS_PATTERN.DEFAULT.DUPLE_SIGUGUN.getPettern(), target);
        log.info("@@ 주소 기본패턴 STEP4 적용 : "+ target);

        //STEP5 - 아파트와 동또는 번지 분리 패턴 적용
        target = getMatchAddBlank(ADDRESS_PATTERN.DEFAULT.APT_BLANK.getPettern(), target);
        log.info("@@ 주소 기본패턴 STEP5 적용 : "+ target);

        //STEP6 - 아파트동과 호 분리
        target = getMatchAddBlank(ADDRESS_PATTERN.DEFAULT.APT_DONG_BLANK.getPettern(), target);
        log.info("@@ 주소 기본패턴 STEP6 적용 : "+ target);

        //STEP7 - 동번지일 경우 띄어쓰기 해주는 패턴 적용
        target = getMatchAddBlank(ADDRESS_PATTERN.DEFAULT.DONG_BUNGI.getPettern(), target);
        log.info("@@ 주소 기본패턴 STEP7 적용 : "+ target);

        //STEP8 - 중복동 제거
        target = getMatchdupleDongOut(ADDRESS_PATTERN.DEFAULT.DUPLE_DONG.getPettern(), target);
        log.info("@@ 주소 기본패턴 STEP8 적용 : "+ target);

        //STEP9 - 번지에 문자가 붙어있을 경우 띄어쓰기 해주는 패턴 적용
        target = getMatchAddBlank(ADDRESS_PATTERN.DEFAULT.BUNGI_BLANK.getPettern(), target);
        log.info("@@ 주소 기본패턴 STEP9 적용 : "+ target);

        //STEP10 - 블랭크 제거
        target = getMatchOutBlank(ADDRESS_PATTERN.DEFAULT.BLANK_OUT.getPettern(), target);
        log.info("@@ 주소 기본패턴 STEP10 적용 : "+ target);

        return target;
    }

    /**
     * 정규식 패턴 매칭(매칭된 패턴과 일치한 것만 가져옴)
     * @Method Name : getMatch
     * @param p
     * @param target
     * @return
     */
    private String getMatch(Pattern p, String target){

        Matcher m = p.matcher(target);

        if (m.find()) target=m.group();

        return target;
    }

    /**
     * 정규식 패턴 매칭(매칭된 패턴 제거)
     * @Method Name : getMatchOut
     * @param p
     * @param target
     * @return
     */
    private String getMatchOut(Pattern p, String target){

        Matcher m = p.matcher(target);

        target =  m.replaceAll(" ");

        return target;
    }

    /**
     * 정규식 패턴 매칭(매칭된 패턴에 블랭크 추가)
     * @Method Name : getMatchAddBlank
     * @param p
     * @param target
     * @return
     */
    private String getMatchAddBlank(Pattern p, String target){

        Matcher m = p.matcher(target);

        while (m.find()) {
            target = m.replaceAll(m.group()+" ");
        }

        return target;
    }

    /**
     * 정규식 패턴 매칭(매칭된 패턴에 블랭크 제거)
     * @Method Name : getMatchOutBlank
     * @param p
     * @param target
     * @return
     */
    private String getMatchOutBlank(Pattern p, String target){

        Matcher m = p.matcher(target);

        while (m.find()) {
            target = m.replaceAll(" ");
        }

        return target;
    }

    /**
     * 정규식 패턴 매칭(루프돌며 매칭된 패턴 모두 제거)
     * @Method Name : getMatchRoopOut
     * @param p
     * @param target
     * @return
     */
    private String getMatchRoopOut(Pattern p, String target){

        Matcher m = p.matcher(target);

        while (m.find()) {
            target = m.replaceAll("");
        }

        return target;
    }

    /**
     * 정규식 패턴 매칭(루프돌며 중복동 제거)
     * @Method Name : getMatchdupleOut
     * @param p
     * @param target
     * @return
     */
    private String getMatchdupleDongOut(Pattern p, String target){

        Matcher m = p.matcher(target);

        int cnt = 0;
        String tmpStr = "";
        String dongArrStr = "";

        while (m.find()) {
            if(cnt == 0){
                //첫번째 동을 tmpStr에 담는다
                tmpStr = m.group();
                target = m.replaceFirst("TEMP_STR");
                dongArrStr=tmpStr;
            }
            else{
                //첫번째 동과 같으면 제거
                if(m.group().equals(tmpStr))
                    target = target.replace(m.group(), "");
                else{//다르면 | 구분자로 문자열 담는다
                    dongArrStr=String.join("@",dongArrStr,m.group());
                    target = target.replaceAll(m.group(), "");
                }
            }
            cnt++;
        }

        if(!dongArrStr.equals(tmpStr))
            dongArr = dongArrStr.split("@");

        //tmpStr이 있으면 TEMP_STR로 변환시킨 첫번째 동을 다시 원복시킨다
        target = target.replaceAll("TEMP_STR", tmpStr);

        return target;
    }

    /**
     * 정규식 패턴 매칭(루프돌며 배열에 저장된 동으로 주소를 변환)
     * @Method Name : getMatchDongRoop
     * @param p
     * @param target
     * @param netxDong
     * @return
     */
    private String getMatchDongRoop(String target, String netxDong){

        Matcher m = ADDRESS_PATTERN.DEFAULT.DUPLE_DONG.getPettern().matcher(target);

        if (m.find()) target = target.replaceAll(m.group(), netxDong);

        return target;
    }

    /**
     * 정규식 패턴 매칭(루프돌며 중복 패턴 제거)
     * @Method Name : getMatchdupleOut
     * @param p
     * @param target
     * @return
     */
    private String getMatchdupleOut(Pattern p, String target){

        Matcher m = p.matcher(target);

        int cnt = 0;
        String tmpStr = "";

        while (m.find()) {
            if(cnt == 0){
                tmpStr = m.group();
                target = m.replaceFirst("TEMP_STR");
            }
            else{
                if(tmpStr.length() < m.group().length()) { //첫번째 조건값 보다 길이가 길면
                    log.debug("length Diff : "+(m.group().length() - tmpStr.length()));
                    if(m.group().length() - tmpStr.length() > 1) //길이 차이가 1보다 크면
                        tmpStr = m.group();
                }
                target = target.replace(m.group(), "");
            }
            cnt++;
        }

        if(!StringUtils.isEmpty(tmpStr))
            target = target.replaceAll("TEMP_STR", tmpStr);

        return target;
    }

    /**
     * 정규식 패턴 매칭(매칭되는 패턴에 의해 정확도 확인)
     * @Method Name : getMatchXyAccType
     * @param p
     * @param target
     * @return
     */
    private boolean getMatchXyAccType(Pattern p, String target){
        boolean result = false;
        Matcher m = p.matcher(target);

        result = m.matches();

        log.debug("## getMatchXyAccType target : "+target+" | result : "+result);
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

        //검색할 주소가 3보다 작으면 null
        if (address.length() < 3)
            return null;

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

    /**
     * 뒤에서부터 공백을 기준으로 한블럭씩 잘라냄 길이 설정가능
     * @Method Name : nextAddr
     * @param address
     * @return
     */
    private String nextAddr(String address, int length) {
        log.debug("# Keyword address.length() : "+address.length());

        if (address.length() < length)
            return null;

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

    /**
     * 앞에서부터 공백을 기준으로 한블럭씩 잘라냄
     * @Method Name : beforeAddr
     * @param address
     * @return
     */
    private String beforeAddr(String address) {
        log.debug("# Address address.length() : "+address.length());

        //검색할 주소가 3보다 작으면 null
        if (address.length() < 3)
            return null;

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
        for (int i = 1; i < temp.length; i++) {
            stringBuilder.append(temp[i]);
            stringBuilder.append(" ");
        }

        return stringBuilder.toString().trim();
    }

    /**
     * 앞에서부터 공백을 기준으로 한블럭씩 잘라냄 길이 설정가능
     * @Method Name : beforeAddr
     * @param address
     * @return
     */
    private String beforeAddr(String address, int length) {
        log.debug("# Keyword address.length() : "+address.length());

        if (address.length() < length)
            return null;

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
        for (int i = 1; i < temp.length; i++) {
            stringBuilder.append(temp[i]);
            stringBuilder.append(" ");
        }

        return stringBuilder.toString().trim();
    }

}
