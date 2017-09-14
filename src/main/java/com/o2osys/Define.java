package com.o2osys;

public interface Define {

    // reqeust parameter 변수 명 정의
    interface Param {
        String LANG_CODE = "lang_code"; // lang code
    }

    String CONTENT_TYPE = "application/json; charset=utf-8";
    String LANG_CODE = "ko";

    // Http 응답 코드 정의
    interface HttpStatus {
        interface Code {
            int OK = 200;
            int BAD_REQUEST = 400;
            int UNAUTHORIZED = 401;
            int NOT_FOUND = 404;
            int SERVER_ERROR = 500;
        }

        interface Message {
            String OK = "성공";
            String BAD_REQUEST = "Bad Request-field validation 실패";
            String UNAUTHORIZED = "Unauthorized-API 인증,인가 실패";
            String NOT_FOUND = "Not Found-해당 리소스가 없음";
            String SERVER_ERROR = "Internal Server Error-서버에러";
        }
    }

    // GPS 정확도
    interface XyAccType {
        /** 정확도 높음 : 1 */
        String TYPE_1 = "1";
        /** 정확도 중간 : 2 */
        String TYPE_2 = "2";
        /** 정확도 낮음 : 3 */
        String TYPE_3 = "3";
    }

    // 다음 API
    interface DaumHttpStatus {
        interface Code {
            int TOO_MANY_REQUEST = 429;
        }

        interface Message {
            String TOO_MANY_REQUEST = "TOO MANY REQUEST";
        }
    }

    // 유니타스 연동 결과코드
    interface UnitasResult {
        interface Code {
            String C0000 = "0000";
            String C0001 = "0001";
            String C0002 = "0002";
            String C0003 = "0003";
            String C0004 = "0004";
            String C0005 = "0005";
            String C9000 = "9000";
            String C9999 = "9999";
        }

        interface MessageCode {
            String C0000 = "unitas.code.0000";
            String C0001 = "unitas.code.0001";
            String C0002 = "unitas.code.0002";
            String C0003 = "unitas.code.0003";
            String C0004 = "unitas.code.0004";
            String C0005 = "unitas.code.0005";
            String C9000 = "unitas.code.9000";
            String C9001 = "unitas.code.9001";
            String C9002 = "unitas.code.9002";
            String C9999 = "unitas.code.9999";
        }
    }
}
