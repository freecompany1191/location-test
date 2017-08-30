package com.o2osys.common.constants;

import java.util.regex.Pattern;

import lombok.Getter;

public interface ADDRESS_PATTERN {
    
	/** 기본 패턴 매칭 그룹 */
	public enum DEFAULT {
		
	 /** 시도 시구군 이 매칭되면 포함 그뒤에 주소들 가져옴 */
	 SIGUGUN( Pattern.compile("(([가-힣]+(시|도)|서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|경북|경남|제주)\\s[가-힣]+(시|군|구).*)"))
	 /** 시도 시구군 읍면동리 or 시도 시구군 or 시도 만 남을 경우 낮음처리 */
	 ,SIDODONG_ONLY(Pattern.compile(
			 "([가-힣]+(시|도)|서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|경북|경남|제주)\\s[가-힣]+(시|군|구)\\s([가-힣]{1,5})+(\\d{1,5}|\\d{1,5}(,|.)\\d{1,5}|)+(읍|면|동|가|리)|"
		    +"([가-힣]+(시|도)|서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|경북|경남|제주)\\s[가-힣]+(시|군|구)|"
		    +"([가-힣]+(시|도)|서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|경북|경남|제주)"
			 ))
	 /** OO동 or 002.3동 or OO2,3동 */
	 ,DUPLE_DONG(Pattern.compile("([가-힣]{1,5})+(\\d{1,5}|\\d{1,5}(,|.)\\d{1,5}|)+(읍|면|동|가|리)\\s", Pattern.DOTALL))
	 /** OO동999-999 or 002.3동999-999 or OO2,3동999-999 */
	 ,DONG_BUNGI(Pattern.compile("([가-힣]{1,5})+(\\d{1,5}|\\d{1,5}(,|.)\\d{1,5}|)+(읍|면|동|가|리)\\d{1,5}(~|-)\\d{1,5}", Pattern.DOTALL))
	 /** 999-999 or 999~999 */
	 ,BUNGI(Pattern.compile("\\d{1,5}(~|-)\\d{1,5}"))
	 ,BUNGI_STR(Pattern.compile("(\\d{1,5}(~|-)\\d{1,5}|\\d{1,5})번지"))
	 ,BRACKET_AREA(Pattern.compile("\\[.*\\]|\\(.*\\)|\\{.*\\}"))
	 ;
		
	  @Getter private final Pattern pettern;
	 
	  DEFAULT(Pattern p) {
	      this.pettern = p;
	  }
	}
	
	/** 스텝별 패턴 매칭 그룹 */
	public enum STEP {
		
	  /** 999-999 or 999~999 */
	  _01( Pattern.compile("\\s\\d{1,5}(~|-)\\d{1,5}\\s")),
	  /** 999-999번지 999~999번지 */
	  _02( Pattern.compile("\\s\\d{1,5}(~|-)\\d{1,5}번지\\s"))
	  ;
		
	  @Getter private final Pattern pettern;
	 
	  STEP(Pattern p) {
	      this.pettern = p;
	  }
	}
	
}