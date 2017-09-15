package com.o2osys.common.constants;

import java.util.regex.Pattern;

import lombok.Getter;

public interface ADDRESS_PATTERN {

    String SIDO = "서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|경북|경남|제주";

    String APT_LIST =
            "아파트|APT|스카이뷰|파크|자이|파크|아이파크|편한세상|래미안|푸르지오|프레스티지|뉴스테이|로프트|단지|차|"
                    +"홈|하이츠|s클래스|S클래스|상떼뷰|보람|모아|타운|연립|빌라|빌|빌리지|맨션|텔|"
                    + "미소가|미래가";

    String DONG_NUM = "\\d{1,5}|\\d{1,5}(,|.)\\d{1,5}";
    String BUNGI_NUM = "\\d{1,5}|\\d{1,5}(~|-)\\d{1,5}";
    String APT_KOR_DONG_STR = "(가|나|다|라|마|바|사|아|자|차|카|타|파|하)";
    String APT_DONG="("+APT_KOR_DONG_STR+"|\\d{1,4}|[a-zA-Z]{1})+동";
    String APT_HO = "(\\d{1,4}|[A-Z]{1}\\d{1,3})호?";

    //제외방법 +(?![제외문자열|제외문자열])(포함문자|포함문자)

    /** 기본 패턴 매칭 그룹 */
    public enum DEFAULT {
        /** STEP1 - [문자] or (문자) or {문자} 제거 */
        BRACKET_STR(Pattern.compile("\\[.*\\]|\\(.*\\)|\\{.*\\}"))
        /** STEP2 - 특수문자 제거 */
        ,SPECIAL_STR(Pattern.compile("[\"\'{}\\[\\];:|\\(\\)*`!_+<>@#$%^&\\=/ㅡ]"))
        /** STEP3 - 시도 시구군 이 매칭되면 포함 그뒤에 주소들 가져옴 */
        ,SIGUGUN( Pattern.compile("(([가-힣]+(시|도)|"+SIDO+")\\s[가-힣]+(시|군|구).*)"))
        /** STEP4 - 시도 시구군 읍면동가리로길 이 중복일때 하나만 남기고 제거 */
        ,DUPLE_SIGUGUN(Pattern.compile("([가-힣]+(시|도)|"+SIDO+")\\s[가-힣]+(시|군|구)\\s(([가-힣]{1,5})+("+DONG_NUM+"|)+(읍|면|동|가|리|로|길)\\s)?"))
        /** STEP5 - 아파트와 동호번지 분리 :: ex)아파트와숫자 or 아파트가동 or 아파트A동 or 아파드726번지 or 아파트108호 패턴이 붙어있을 시 아파트 이름만 가져옴 */
        ,APT_BLANK(Pattern.compile("(([가-힣]\\d{1,5}|[가-힣])?)+(\\s?)+("+APT_LIST+")+(?=(\\d{1,4}|\\d{1,4}(~|-)\\d{1,4}|\\w|"+APT_KOR_DONG_STR+")+(동|번지|호))"))
        /** STEP6 - 아파트동과 호 분리 :: ex)아파트의 가동101호 or A동101호 2동101호 패턴이 붙어있을 시 동이름만 가져옴 */
        ,APT_DONG_BLANK(Pattern.compile("("+APT_DONG+")+(?=("+APT_HO+"))"))
        /** STEP7 - 주소동과 번지 분리 :: ex) OO동999-999 or 002.3동999-999 or OO2,3동999-999 */
        ,DONG_BUNGI(Pattern.compile("([가-힣]{1,5})+("+DONG_NUM+"|)+(읍|면|동|가|리)+(?=("+BUNGI_NUM+")+(번지|))"))
        /** STEP8 - 중복동 제거 :: ex)OO동 or 002.3동 or OO2,3동 */
        ,DUPLE_DONG(Pattern.compile("\\s((?!"+APT_DONG+")([가-힣]{1,5})+("+DONG_NUM+"|)+(동)(?=\\s))"))
        /** STEP9 - 번지와 문자 분리 :: ex)999번지 or 999-999번지에 공백 이외의 문자가 붙어있을때 번지만 가져옴 */
        ,BUNGI_BLANK(Pattern.compile("("+BUNGI_NUM+")+(번지)+(?=(\\w|[가-힣]|\\d{1,5}))"))
        /** STEP10 - 블랭크 제거 */
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
                +"([가-힣]+(시|도)|"+SIDO+")\\s[가-힣]+(시|군|구)\\s([가-힣]{1,5})+("+DONG_NUM+"|)+(읍|면|동|가|리|로|길)|"
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
        _01( Pattern.compile("\\s("+BUNGI_NUM+")+(번지|)\\s"))
        ;

        @Getter private final Pattern pettern;

        STEP(Pattern p) {
            this.pettern = p;
        }
    }

}