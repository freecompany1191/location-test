package com.o2osys.common.constants;

import java.util.regex.Pattern;

import lombok.Getter;

public interface ADDRESS_PATTERN {

    String SIDO = "서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|경북|경남|제주";

    String APT_LIST =
            "아파트|APT|스카이뷰|파크|자이|파크|아이파크|편한세상|래미안|푸르지오|프레스티지|뉴스테이|"
                    + "빌라|타운|빌|빌리지|맨션|텔";

    String DONG_NUM = "\\d{1,5}|\\d{1,5}(,|.)\\d{1,5}";

    /** 기본 패턴 매칭 그룹 */
    public enum DEFAULT {
        /** STEP1 - [문자] or (문자) or {문자} 제거 */
        BRACKET_STR(Pattern.compile("\\[.*\\]|\\(.*\\)|\\{.*\\}"))
        /** STEP2 - 특수문자 제거 */
        ,SPECIAL_STR(Pattern.compile("[\"\'{}\\[\\];:|\\(\\)*`!_+<>@#$%^&\\=/]"))
        /** STEP3 - 시도 시구군 이 매칭되면 포함 그뒤에 주소들 가져옴 */
        ,SIGUGUN( Pattern.compile("(([가-힣]+(시|도)|"+SIDO+")\\s[가-힣]+(시|군|구).*)"))
        /** STEP4 -OO동999-999 or 002.3동999-999 or OO2,3동999-999 */
        ,DONG_BUNGI(Pattern.compile("([가-힣]{1,5})+(\\d{1,5}|\\d{1,5}(,|.)\\d{1,5}|)+(읍|면|동|가|리)+(?=(\\d{1,5}|\\d{1,5}(~|-)\\d{1,5})+(번지|))"))
        /** STEP5 - 아파트와숫자 or 아파트가동 or 아파트A동 or 아파드726번지 or 아파트108호 패턴이 붙어있을 시 아파트 이름만 가져옴 */
        ,APT_BLANK(Pattern.compile("([가-힣](\\d{1,5})?)+("+APT_LIST+")+(?=(\\d{1,5}|\\w|[가-힣])+(동|번지|호))"))
        /** STEP6 - 999번지 or 999-999번지에 공백 이외의 문자가 붙어있을때 번지만 가져옴 */
        ,BUNGI_BLANK(Pattern.compile("(\\d{1,5}|\\d{1,5}(~|-)\\d{1,5})+(번지)+(?=(\\w|[가-힣]|\\d{1,5}))"))
        /** STEP7 - OO동 or 002.3동 or OO2,3동 중복동 제거*/
        ,DUPLE_DONG(Pattern.compile("([가-힣]{1,5})+("+DONG_NUM+"|)+(읍|면|동|가|리)\\s", Pattern.DOTALL))
        /** STEP8 - 블랭크 제거 */
        ,BLANK_OUT(Pattern.compile("\\s{2,}"));
        ;

        @Getter private final Pattern pettern;

        DEFAULT(Pattern p) {
            this.pettern = p;
        }
    }


    /** 조건에 따라 사용하는 매칭 패턴 */
    public enum CUSTOM {
        /** 시도 시구군 읍면동리 or 시도 시구군 or 시도 만 남을 경우 낮음처리 */
        SIDODONG_ONLY(Pattern.compile(""
                +"([가-힣]+(시|도)|"+SIDO+")\\s[가-힣]+(시|군|구)\\s([가-힣]{1,5})+("+DONG_NUM+"|)+(읍|면|동|가|리|)?(로|길|)|"
                +"([가-힣]+(시|도)|"+SIDO+")\\s[가-힣]+(시|군|구)|"
                +"([가-힣]+(시|도)|"+SIDO+")"
                + ""))
        ;

        @Getter private final Pattern pettern;

        CUSTOM(Pattern p) {
            this.pettern = p;
        }
    }

    /** 스텝별 패턴 매칭 그룹 */
    public enum STEP {

        /** 999 999-999 999~999 or 999번지 999-999번지 999~999번지 */
        _01( Pattern.compile("\\s(\\d{1,5}|\\d{1,5}(~|-)\\d{1,5})+(번지|)\\s"))
        ;

        @Getter private final Pattern pettern;

        STEP(Pattern p) {
            this.pettern = p;
        }
    }

}