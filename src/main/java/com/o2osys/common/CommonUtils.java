package com.o2osys.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
   @FileName  : CommonUtil.java
   @Description : 공통유틸
   @author      : KMS
   @since       : 2017. 7. 21.
   @version     : 1.0
  
   @개정이력
   
   수정일           수정자         수정내용
   -----------      ---------      -------------------------------
   2017. 7. 21.     KMS            최초생성
 
 */
@Service
public class CommonUtils {

    /**
     * Object -> JSON 형식으로 변환
     * 
     * @Method Name : jsonStringFromObject
     * @param object
     * @return
     * @throws JsonProcessingException
     */
    public String jsonStringFromObject(Object object) throws JsonProcessingException {
        
        if(object == null){
            return null;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }
    
    /**
     * 날짜 형식 변경
     * yyyyMMddHHmmss -> yyyy-MM-dd HH:mm:ss
     * @Method Name : strToDate
     * @param strDate
     * @return
     */
    public String strToDate(String strDate) {
        
        if(StringUtils.isEmpty(strDate)){
            return null;
        }
        
        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.KOREA);
        LocalDateTime toDate = LocalDateTime.parse(strDate,pattern);
        
        pattern = DateTimeFormatter.ofPattern("yyyy.MM.dd HH;mm;ss", Locale.KOREA);
        String formatStr = pattern.format(toDate);
        
        return formatStr;
    }
}
