package com.example.mealdangapi.recommend;

import java.util.List;

/**
 * Hibernate의 JSON 컬럼 자동 매핑(FormatMapper)이 현재 환경(Jackson 3)에서 동작하지 않아,
 * 문자열 리스트를 JSON 배열 문자열로 직접 변환하는 용도의 최소 유틸.
 */
final class JsonArrayUtil {

    private JsonArrayUtil() {
    }

    static String toJsonArray(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            String escaped = items.get(i).replace("\\", "\\\\").replace("\"", "\\\"");
            sb.append('"').append(escaped).append('"');
        }
        return sb.append("]").toString();
    }
}
